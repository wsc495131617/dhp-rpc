package org.dhp.net.nio;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.core.rpc.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@Slf4j
public class NioRpcChannel extends RpcChannel {

    SocketChannel socketChannel;

    static Selector selector;

    ClientStreamManager streamManager;

    static synchronized Selector getSelector() {
        if (selector == null) {
            try {
                selector = Selector.open();
                Thread thread = new Thread(() -> {
                    while (true) {
                        try {
                            selector.select();
                            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                            while (it.hasNext()) {
                                SelectionKey key = it.next();
                                it.remove();
                                dealSelectionKey(key);
                            }
                        } catch (IOException e) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException interruptedException) {
                            }
                        }
                    }
                });
                thread.setName("MainClientLoop");
                thread.setDaemon(true);
                thread.start();
            } catch (IOException e) {
            }
        }
        return selector;
    }

    static Map<SocketChannel, NioRpcChannel> channelMap = new ConcurrentHashMap<>();

    static void dealSelectionKey(SelectionKey key) {
        if (key.isReadable()) {
            SocketChannel socket = (SocketChannel) key.channel();
            //socket 读取 bytes到session里面
            NioRpcChannel channel = channelMap.get(socket);
            channel.readMessage();
        }
    }


    MessageDecoder messageDecoder;

    @Override
    public void start() {
        try {
            messageDecoder = new BufferMessageDecoder(256);
            connect();
        } catch (TimeoutException e) {
        }
    }

    protected boolean readMessage() {
        List<NioMessage> messages = new LinkedList<>();
        if (!messageDecoder.read(socketChannel, messages)) {
            //关闭了
            this.active = false;
            readyToCloseConns.remove(socketChannel);
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
                socketChannel.configureBlocking(false);
                socketChannel.register(getSelector(), SelectionKey.OP_CONNECT);
                //连接
                socketChannel.connect(new InetSocketAddress(this.getHost(), this.getPort()));
                channelMap.put(socketChannel, this);
            }
            return register();
        } catch (IOException e) {
            return false;
        }
    }

    public NioMessage sendMessage(String command, byte[] body) throws IOException, TimeoutException {
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

        NioMessage message = new NioMessage();
        message.setId(_ID.incrementAndGet());
        message.setCommand(command);
        message.setData(body);
        message.setStatus(MessageStatus.Sending);
        this.socketChannel.write(message.pack());
        return message;
    }

    @Override
    public Integer write(String name, byte[] argBody, Stream<Message> messageStream) {
        NioMessage message = null;
        try {
            message = sendMessage(name, argBody);
            streamManager.setStream(message, messageStream);
            return message.getId();
        } catch (IOException | TimeoutException e) {
            return 0;
        }
    }

    @Override
    public void close() {

    }
}
