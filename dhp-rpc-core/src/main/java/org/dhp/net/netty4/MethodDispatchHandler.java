package org.dhp.net.netty4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.common.utils.ProtostuffUtils;
import org.dhp.core.rpc.*;

/**
 * @author zhangcb
 */
@Slf4j
public class MethodDispatchHandler extends ChannelInboundHandlerAdapter {

    RpcServerMethodManager methodManager;

    SessionManager sessionManager;

    public MethodDispatchHandler(RpcServerMethodManager methodManager, SessionManager sessionManager) {
        this.methodManager = methodManager;
        this.sessionManager = sessionManager;
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        sessionManager.destorySession(ctx.channel());
        super.handlerRemoved(ctx);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;
        Session session = sessionManager.getSession(ctx.channel());
        //等待注册
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
                log.warn("收到未注册消息，丢弃: {}, 并关闭: {}", message, ctx.channel());
                ctx.channel().closeFuture();
            }
            ctx.fireChannelReadComplete();
            return;
        }

        ServerCommand command = methodManager.getCommand(message.getCommand());
        Stream stream = new NettyStream(session.getId(), command, message);
        Workers.getExecutorService(message).execute(command, stream, message, session);
        ctx.fireChannelReadComplete();
    }

    class NettyStream<T> implements Stream<T> {

        Long sessionId;
        ServerCommand command;
        Message message;

        public NettyStream(Long sessionId, ServerCommand command, Message message) {
            this.sessionId = sessionId;
            this.command = command;
            this.message = message;
        }

        public void onCanceled() {
            NettyMessage retMessage = new NettyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Canceled);
            retMessage.setMetadata(message.getMetadata());
            retMessage.setCommand(command.getName());
            Session session = sessionManager.getSessionById(sessionId);
            session.write(retMessage);
        }

        public void onNext(Object value) {
            NettyMessage retMessage = new NettyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Updating);
            retMessage.setCommand(command.getName());
            retMessage.setMetadata(message.getMetadata());
            retMessage.setData(MethodDispatchUtils.dealResult(command, value));
            Session session = sessionManager.getSessionById(sessionId);
            session.write(retMessage);
        }

        public void onFailed(Throwable throwable) {
            NettyMessage retMessage = new NettyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Failed);
            retMessage.setCommand(command.getName());
            retMessage.setData(MethodDispatchUtils.dealFailed(command, throwable));
            retMessage.setMetadata(message.getMetadata());
            Session session = sessionManager.getSessionById(sessionId);
            session.write(retMessage);
        }

        public void onCompleted() {
            NettyMessage retMessage = new NettyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Completed);
            retMessage.setMetadata(message.getMetadata());
            retMessage.setCommand(command.getName());
            Session session = sessionManager.getSessionById(sessionId);
            session.write(retMessage);
        }
    }

}
