package org.dhp.core.netty4;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.CompleteHandler;
import org.dhp.common.rpc.ListenableFuture;
import org.dhp.common.rpc.Stream;
import org.dhp.common.utils.ProtostuffUtils;
import org.dhp.core.rpc.MessageStatus;
import org.dhp.core.rpc.MethodType;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.dhp.core.rpc.ServerCommand;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Slf4j
public class MethodDispatchHandler extends ChannelInboundHandlerAdapter {

    RpcServerMethodManager methodManager;

    public MethodDispatchHandler(RpcServerMethodManager methodManager) {
        this.methodManager = methodManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NettyMessage message = (NettyMessage) msg;
        ServerCommand command = methodManager.getCommand(message.getCommand());
        Type[] paramTypes = command.getMethod().getParameterTypes();

        if (command.getType() == MethodType.Stream) {// call(req, stream<resp>)
            Stream stream = new Stream() {
                public void onCanceled() {
                    NettyMessage retMessage = new NettyMessage();
                    retMessage.setId(message.getId());
                    retMessage.setStatus(MessageStatus.Canceled);
                    retMessage.setMetadata(message.getMetadata());
                    retMessage.setCommand(command.getName());
                    ctx.channel().writeAndFlush(retMessage);
                }

                public void onNext(Object value) {
                    NettyMessage retMessage = new NettyMessage();
                    retMessage.setId(message.getId());
                    retMessage.setStatus(MessageStatus.Updating);
                    retMessage.setCommand(command.getName());
                    retMessage.setMetadata(message.getMetadata());
                    retMessage.setData(dealResult(command, value));
                    ctx.channel().writeAndFlush(retMessage);
                }

                public void onFailed(Throwable throwable) {
                    NettyMessage retMessage = new NettyMessage();
                    retMessage.setId(message.getId());
                    retMessage.setStatus(MessageStatus.Failed);
                    retMessage.setCommand(command.getName());
                    retMessage.setData(dealFailed(command, throwable));
                    retMessage.setMetadata(message.getMetadata());
                    ctx.channel().writeAndFlush(retMessage);
                }

                public void onCompleted() {
                    NettyMessage retMessage = new NettyMessage();
                    retMessage.setId(message.getId());
                    retMessage.setStatus(MessageStatus.Completed);
                    retMessage.setMetadata(message.getMetadata());
                    retMessage.setCommand(command.getName());
                    ctx.channel().writeAndFlush(retMessage);
                }
            };
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
            try {
                result = command.getMethod().invoke(command.getBean(), new Object[]{param});
            } catch (RuntimeException e) {
                log.error(e.getMessage(), e);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                log.error(cause.getMessage(), cause);
            }
            NettyMessage retMessage = new NettyMessage();
            if (command.getType() == MethodType.Default) {// resp call(req)
                retMessage.setId(message.getId());
                retMessage.setStatus(MessageStatus.Completed);
                retMessage.setMetadata(message.getMetadata());
                retMessage.setData(dealResult(command, result));
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
                    ListenableFuture<Object> future = (ListenableFuture) result;
                    future.addCompleteHandler(new NettyCompleteHandler(message, ctx.channel(), command));
                }
            }
        }
        ctx.fireChannelReadComplete();
    }

    public class NettyCompleteHandler implements CompleteHandler<Object> {
        NettyMessage message;
        Channel connection;
        ServerCommand command;

        public NettyCompleteHandler(NettyMessage message, Channel connection, ServerCommand command) {
            this.message = message;
            this.connection = connection;
            this.command = command;
        }


        public void onCompleted(Object result) {
            NettyMessage retMessage = new NettyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Completed);
            retMessage.setMetadata(message.getMetadata());
            retMessage.setData(dealResult(command, result));
            retMessage.setCommand(command.getName());
            connection.writeAndFlush(retMessage);
        }

        public void onCanceled() {
            NettyMessage retMessage = new NettyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Canceled);
            retMessage.setMetadata(message.getMetadata());
            retMessage.setCommand(command.getName());
            connection.writeAndFlush(retMessage);
        }

        public void onFailed(Throwable e) {
            NettyMessage retMessage = new NettyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Failed);
            retMessage.setMetadata(message.getMetadata());
            retMessage.setCommand(command.getName());
            retMessage.setData(dealFailed(command, e));
            connection.writeAndFlush(retMessage);
        }
    }

    private byte[] dealFailed(ServerCommand command, Throwable e) {
        return null;
    }

    private byte[] dealResult(ServerCommand command, Object result) {
        try {
            if (command.getType() == MethodType.Default) {
                return ProtostuffUtils.serialize((Class) command.getMethod().getReturnType(), result);
            } else if (command.getType() == MethodType.Future) {
                ParameterizedType type = (ParameterizedType) command.getMethod().getGenericReturnType();
                Class clas = (Class) type.getActualTypeArguments()[0];
                return ProtostuffUtils.serialize(clas, result);
            } else if (command.getType() == MethodType.Stream) {
                Type[] paramTypes = command.getMethod().getParameterTypes();
                if (Stream.class.isAssignableFrom((Class) paramTypes[0])) {
                    return ProtostuffUtils.serialize((Class) paramTypes[1], result);
                } else {
                    return ProtostuffUtils.serialize((Class) paramTypes[0], result);
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
