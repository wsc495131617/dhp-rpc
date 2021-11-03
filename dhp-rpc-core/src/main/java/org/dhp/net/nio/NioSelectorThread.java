package org.dhp.net.nio;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.core.rpc.*;
import org.dhp.net.BufferMessage;

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
            List<BufferMessage> messages = new LinkedList<>();
            if (!session.getMessageDecoder().read((SocketChannel) event.channel(), messages)) {
                session.destroy();
                event.cancel();
                return false;
            }
            for (BufferMessage message : messages) {
                dealMessage(session, message);
            }
        } catch (Throwable e) {
            log.error(e.getLocalizedMessage(), e);
        }
        return true;

    }

    protected void dealMessage(NioSession session, BufferMessage message) {
        ServerCommand command = methodManager.getCommand(message.getCommand());
        Stream stream = new NioStream(session.getId(), command, message);
        Workers.getWorker(message).execute(command, stream, message, session);
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
            BufferMessage retMessage = new BufferMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Canceled);
            retMessage.setMetadata(message.getMetadata());
            retMessage.setCommand(command.getName());
            Session session = sessionManager.getSessionById(sessionId);
            session.write(retMessage);
        }

        public void onNext(Object value) {
            BufferMessage retMessage = new BufferMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Updating);
            retMessage.setCommand(command.getName());
            retMessage.setMetadata(message.getMetadata());
            retMessage.setData(MethodDispatchUtils.dealResult(command, value));
            Session session = sessionManager.getSessionById(sessionId);
            session.write(retMessage);
        }

        public void onFailed(Throwable throwable) {
            BufferMessage retMessage = new BufferMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Failed);
            retMessage.setCommand(command.getName());
            retMessage.setData(MethodDispatchUtils.dealFailed(command, throwable));
            retMessage.setMetadata(message.getMetadata());
            Session session = sessionManager.getSessionById(sessionId);
            session.write(retMessage);
        }

        public void onCompleted() {
            BufferMessage retMessage = new BufferMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Completed);
            retMessage.setMetadata(message.getMetadata());
            retMessage.setCommand(command.getName());
            Session session = sessionManager.getSessionById(sessionId);
            session.write(retMessage);
        }
    }
}
