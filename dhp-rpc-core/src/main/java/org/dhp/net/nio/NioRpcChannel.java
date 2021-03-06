package org.dhp.net.nio;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.core.rpc.*;
import org.dhp.net.BufferMessage;
import org.glassfish.grizzly.Buffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class NioRpcChannel extends RpcChannel {

    SocketChannel socketChannel;

    ClientStreamManager streamManager;

    Selector selector;

    AtomicBoolean running = new AtomicBoolean(false);

    Selector getSelector() {
        if (selector == null) {
            try {
                selector = Selector.open();
                Thread thread = new Thread(() -> {
                    while (running.get()) {
                        try {
                            selector.select(1000);
                            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                            while (it.hasNext()) {
                                SelectionKey key = it.next();
                                it.remove();
                                dealSelectionKey(key);
                            }
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            try {
                                Thread.sleep(10);
                            } catch(Exception e1){}
                        }
                    }
                });
                thread.setName("NIOClientLoop-" + getId());
                thread.setDaemon(true);
                thread.start();
            } catch (IOException e) {
            }
        }
        return selector;
    }

    static void dealSelectionKey(SelectionKey key) {
        if (key.isReadable() && key.isValid()) {
            NioRpcChannel channel = (NioRpcChannel) key.attachment();
            channel.readMessage();
        }
    }


    MessageDecoder messageDecoder;

    @Override
    public void start() {
        try {
            running.set(true);
            messageDecoder = new MessageDecoder(2048);
            streamManager = new ClientStreamManager();
            connect();
        } catch (TimeoutException e) {
        }
    }

    protected boolean readMessage() {
        if (socketChannel == null) {
            return false;
        }
        List<BufferMessage> messages = new LinkedList<>();
        if (!messageDecoder.read(socketChannel, messages)) {
            //?????????
            this.active = false;
            readyToCloseConns.remove(socketChannel);
            this.socketChannel = null;
            return false;
        }
        messages.forEach(message -> {
            activeTime = System.currentTimeMillis();
            if (message.getCommand().equals("close")) {
                readyToCloseConns.add(socketChannel);
                active = false;
            } else {
                streamManager.handleMessage(message);
            }
        });
        return true;
    }

    @Override
    public boolean isClose() {
        return socketChannel == null || !socketChannel.isConnected();
    }

    @Override
    public boolean connect() throws TimeoutException {
        try {
            if (socketChannel == null || !socketChannel.isConnected()) {
                socketChannel = SocketChannel.open();
                socketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                socketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                socketChannel.connect(new InetSocketAddress(this.getHost(), this.getPort()));
                socketChannel.configureBlocking(false);
                socketChannel.register(getSelector(), SelectionKey.OP_READ, this);
            }
            this.active = true;
            this.activeTime = System.currentTimeMillis();
            return true;
        } catch (IOException e) {
            log.error("connet error {}", e.getMessage());
            return false;
        }
    }

    protected BufferMessage createMessage(String command, byte[] body) {
        BufferMessage message = new BufferMessage();
        message.setId(_ID.incrementAndGet());
        message.setCommand(command);
        message.setData(body);
        message.setStatus(MessageStatus.Sending);
        return message;
    }

    public BufferMessage sendMessage(BufferMessage message) throws IOException, TimeoutException {
        synchronized (socketChannel) {
            while (readyToCloseConns.contains(socketChannel)) {
                try {
                    log.warn("waiting for switch channel");
                    connect();
                    log.warn("switch channel success!");
                } catch (RpcException e) {
                    if (e.getCode() == RpcErrorCode.UNREACHABLE_NODE) {
                        continue;
                    }
                } finally {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        Buffer buffer = message.pack();
        this.socketChannel.write(buffer.toByteBuffer());
        buffer.dispose();
        return message;
    }

    @Override
    public Integer write(String name, byte[] argBody, Stream<Message> messageStream) {
        BufferMessage message = createMessage(name, argBody);
        try {
            streamManager.setStream(message, messageStream);
            sendMessage(message);
            return message.getId();
        } catch (IOException | TimeoutException e) {
            streamManager.clearId(message.getId());
            return 0;
        }
    }

    @Override
    public void close() {
        log.info("close channel: {}", this);
        this.running.set(false);
    }

}
