package org.dhp.net.nio;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.common.utils.ProtostuffUtils;
import org.dhp.core.rpc.*;
import org.dhp.net.netty4.NettyMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class NioRpcSocketServer implements IRpcServer, Runnable {

    int port;
    int workThread;
    Selector selector;
    ServerSocketChannel serverSocketChannel;
    NioSessionManager sessionManager;
    RpcServerMethodManager methodManager;

    public NioRpcSocketServer(int port, int workThread,RpcServerMethodManager methodManager) {
        this.port = port;
        this.workThread = workThread;
        this.sessionManager = new NioSessionManager();
    }

    @Override
    public void start(RpcServerMethodManager methodManager) throws IOException {
        this.methodManager = methodManager;
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(port));
        selector = Selector.open();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        log.info("start at {}", port);
        //开启主循环
        Thread mainThread = new Thread(this);
        mainThread.setName("NioMainLoop");
        mainThread.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    //新进入连接
                    if (key.isAcceptable()) {
                        SocketChannel socket = serverSocketChannel.accept();
                        socket.configureBlocking(false);
                        //注册
                        socket.register(selector, SelectionKey.OP_READ);
                        addChannel(socket);
                    }
                    if (key.isReadable()) {
                        SocketChannel socket = (SocketChannel) key.channel();
                        if(!readChannel(socket)){
                            key.cancel();
                            continue;
                        }
                    }
                    if (key.isWritable()) {
                        log.info("write:{}", key.channel());
                    }
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    protected void addChannel(SocketChannel socketChannel) {
        NioSession session = (NioSession)sessionManager.getSession(socketChannel);
        session.setMessageDecoder(new BufferMessageDecoder(256));
    }

    //读取连接数据
    protected boolean readChannel(SocketChannel socket) {
        try {
            NioSession session = (NioSession)sessionManager.getSession(socket);
            //socket 读取 bytes到session里面
            List<NioMessage> messages = new LinkedList<>();
            if(!session.getMessageDecoder().read(socket, messages)){
                session.destroy();
                return false;
            }
            for(NioMessage message : messages) {
                dealMessage(session, message);
            }
        } catch (Throwable e) {
            log.error(e.getLocalizedMessage(), e);
        }
        return true;

    }

    protected void dealMessage(NioSession session, NioMessage message) {
        log.info("dealMessage: {}", message);
        if(!session.isRegister()) {
            if (message.getCommand().equalsIgnoreCase("register")) {
                session.setId(ProtostuffUtils.deserialize(message.getData(), Long.class));
                if (sessionManager.register(session)) {
                    message.setStatus(MessageStatus.Completed);
                } else {
                    message.setStatus(MessageStatus.Failed);
                }
                session.write(message);
            } else {
                log.warn("收到未注册消息，丢弃: {}, 并关闭: {}", message, session);
            }
            return;
        }
        ServerCommand command = methodManager.getCommand(message.getCommand());
        if(command == null) {
            if(message.getCommand().equalsIgnoreCase("ping")){
                NioMessage retMessage = new NioMessage();
                retMessage.setId(message.getId());
                retMessage.setStatus(MessageStatus.Completed);
                retMessage.setCommand(message.getCommand());
                retMessage.setData((System.currentTimeMillis()+"").getBytes());
                session.write(retMessage);
            } else {
                NettyMessage retMessage = new NettyMessage();
                retMessage.setId(message.getId());
                retMessage.setStatus(MessageStatus.Failed);
                retMessage.setCommand(message.getCommand());
                retMessage.setData("no command".getBytes());
                session.write(retMessage);
            }
        } else {
            Stream stream = new NioStream(session.getId(), command, message);
            Workers.getExecutorService(message).execute(command, stream, message, session);
        }
    }

    @Override
    public void running() {

    }

    @Override
    public void shutdown() {

    }

    class NioStream<T> implements Stream<T> {

        Long sessionId;
        ServerCommand command;
        Message message;

        public NioStream(Long sessionId, ServerCommand command, Message message){
            this.sessionId = sessionId;
            this.command = command;
            this.message = message;
        }

        public void onCanceled() {
            NioMessage retMessage = new NioMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Canceled);
            retMessage.setMetadata(message.getMetadata());
            retMessage.setCommand(command.getName());
            Session session = sessionManager.getSessionById(sessionId);
            session.write(retMessage);
        }

        public void onNext(Object value) {
            NioMessage retMessage = new NioMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Updating);
            retMessage.setCommand(command.getName());
            retMessage.setMetadata(message.getMetadata());
            retMessage.setData(MethodDispatchUtils.dealResult(command, value));
            Session session = sessionManager.getSessionById(sessionId);
            session.write(retMessage);
        }

        public void onFailed(Throwable throwable) {
            NioMessage retMessage = new NioMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Failed);
            retMessage.setCommand(command.getName());
            retMessage.setData(MethodDispatchUtils.dealFailed(command, throwable));
            retMessage.setMetadata(message.getMetadata());
            Session session = sessionManager.getSessionById(sessionId);
            session.write(retMessage);
        }

        public void onCompleted() {
            NioMessage retMessage = new NioMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Completed);
            retMessage.setMetadata(message.getMetadata());
            retMessage.setCommand(command.getName());
            Session session = sessionManager.getSessionById(sessionId);
            session.write(retMessage);
        }
    }
}
