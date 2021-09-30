package org.dhp.net.nio;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.common.utils.ProtostuffUtils;
import org.dhp.core.rpc.*;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * 专门用于Read的Selector
 */
@Slf4j
public class NioSelectorThread extends Thread {

    public RpcServerMethodManager methodManager;
    public NioSessionManager sessionManager;

    Selector selector;

    final BlockingQueue<SocketChannel> acceptQueue;

    public NioSelectorThread(BlockingQueue<SocketChannel> acceptQueue) {
        this.acceptQueue = acceptQueue;
        try {
            selector = Selector.open();
        } catch (IOException e) {
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    //有消息
                    if (key.isReadable()) {
                        readChannel(key);
                    }
                }
                SocketChannel socketChannel = acceptQueue.poll();
                if (socketChannel != null) {
                    //通过direct内存进行消息的缓存，作为服务端接受的命令，一般都很小
                    NioSession session = new NioSession(socketChannel);
                    session.setMessageDecoder(new MessageDecoder(1024));
                    socketChannel.register(selector, SelectionKey.OP_READ, session);
                    log.info("new session {}", session);
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    //读取连接数据
    protected boolean readChannel(SelectionKey event) {
        try {
            NioSession session = (NioSession) event.attachment();
            //socket 读取 bytes到session里面
            List<NioMessage> messages = new LinkedList<>();
            if (!session.getMessageDecoder().read((SocketChannel) event.channel(), messages)) {
                session.destroy();
                event.cancel();
                return false;
            }
            for (NioMessage message : messages) {
                dealMessage(session, message);
            }
        } catch (Throwable e) {
            log.error(e.getLocalizedMessage(), e);
        }
        return true;

    }

    protected void dealMessage(NioSession session, NioMessage message) {
        if (!session.isRegister()) {
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
        Stream stream = new NioStream(session.getId(), command, message);
        Workers.getExecutorService(message).execute(command, stream, message, session);
    }

    class NioStream<T> implements Stream<T> {

        Long sessionId;
        ServerCommand command;
        Message message;

        public NioStream(Long sessionId, ServerCommand command, Message message) {
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
