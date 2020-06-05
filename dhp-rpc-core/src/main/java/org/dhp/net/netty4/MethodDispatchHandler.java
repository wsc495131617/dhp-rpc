package org.dhp.net.netty4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.common.rpc.StreamFuture;
import org.dhp.common.utils.ProtostuffUtils;
import org.dhp.core.rpc.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

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
        if(!session.isRegister()){
            if(message.getCommand().equalsIgnoreCase("register")){
                session.setId(ProtostuffUtils.deserialize(message.getData(), Long.class));
                if(sessionManager.register(session)){
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
        if(command == null) {
            if(message.getCommand().equalsIgnoreCase("ping")){
                NettyMessage retMessage = new NettyMessage();
                retMessage.setId(message.getId());
                retMessage.setStatus(MessageStatus.Completed);
                retMessage.setCommand(message.getCommand());
                retMessage.setData((System.currentTimeMillis()+"").getBytes());
                ctx.channel().write(retMessage);
            } else {
                NettyMessage retMessage = new NettyMessage();
                retMessage.setId(message.getId());
                retMessage.setStatus(MessageStatus.Failed);
                retMessage.setCommand(message.getCommand());
                retMessage.setData("no command".getBytes());
                ctx.channel().write(retMessage);
            }
        } else {
            Type[] paramTypes = command.getMethod().getParameterTypes();
            Stream stream = new NettyStream(session.getId(), command, message);
            if (command.getType() == MethodType.Stream) {// call(req, stream<resp>)
                Object[] params;
                if (Stream.class.isAssignableFrom((Class<?>) paramTypes[0])) {
                    params = new Object[]{stream, ProtostuffUtils.deserialize(message.getData(), (Class<?>) paramTypes[1])};
                } else {
                    params = new Object[]{ProtostuffUtils.deserialize(message.getData(), (Class<?>) paramTypes[0]), stream};
                }
                try {
                    command.getMethod().invoke(command.getBean(), params);
                } catch (RuntimeException e) {
                    log.error(e.getMessage(), e);
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    log.error(cause.getMessage(), cause);
                }
            } else {
                Object param = ProtostuffUtils.deserialize(message.getData(), (Class<?>) paramTypes[0]);
                Object result = null;
                Throwable throwable = null;
                try {
                    result = command.getMethod().invoke(command.getBean(), new Object[]{param});
                } catch (RuntimeException e) {
                    log.error(e.getMessage(), e);
                    throwable = e;
                } catch (IllegalAccessException e) {
                    throwable = e;
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    log.error(cause.getMessage(), cause);
                    throwable = cause;
                }

                NettyMessage retMessage = new NettyMessage();
                if (command.getType() == MethodType.Default || command.getType() == MethodType.List) {// resp call(req)
                    retMessage.setId(message.getId());
                    if (throwable != null) {
                        retMessage.setStatus(MessageStatus.Failed);
                        retMessage.setData(MethodDispatchUtils.dealFailed(command, throwable));
                    } else {
                        retMessage.setStatus(MessageStatus.Completed);
                        retMessage.setData(MethodDispatchUtils.dealResult(command, result));
                    }
                    retMessage.setMetadata(message.getMetadata());
                    retMessage.setCommand(command.getName());
                    ctx.channel().writeAndFlush(retMessage);
                } else if (command.getType() == MethodType.Future) {// future<resp> call(req)
                    if (result == null) {
                        retMessage.setId(message.getId());
                        retMessage.setStatus(MessageStatus.Completed);
                        retMessage.setMetadata(message.getMetadata());
                        retMessage.setCommand(command.getName());
                        ctx.channel().writeAndFlush(retMessage);
                    } else {
                        StreamFuture<Object> future = (StreamFuture) result;
                        future.addStream(stream);
                        //加入到session管理里面，当session销毁，异步future就需要被cancel
                        session.addFuture(future);

                    }
                }
            }
        }
        ctx.fireChannelReadComplete();
    }

    class NettyStream<T> implements Stream<T> {

        Long sessionId;
        ServerCommand command;
        Message message;

        public NettyStream(Long sessionId, ServerCommand command, Message message){
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
