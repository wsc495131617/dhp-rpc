package org.dhp.core.grizzly;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.utils.ProtostuffUtils;
import org.dhp.core.rpc.*;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

@Slf4j
public class MethodDispatchFilter extends BaseFilter {
    RpcServerMethodManager methodManager;

    public MethodDispatchFilter(RpcServerMethodManager methodManager) {
        this.methodManager = methodManager;
    }

    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        GrizzlyMessage message = ctx.getMessage();
        ServerCommand command = methodManager.getCommand(message.getCommand());
        Type[] paramTypes = command.getMethod().getParameterTypes();

        if(command.getType() == MethodType.Stream) {// call(req, stream<resp>)
            Stream stream = new Stream() {
                public void onCanceled() {
                    GrizzlyMessage retMessage = new GrizzlyMessage();
                    retMessage.setId(message.getId());
                    retMessage.setStatus(MessageStatus.Canceled);
                    retMessage.setMetadata(message.getMetadata());
                    retMessage.setCommand(command.getName());
                    ctx.getConnection().write(retMessage);
                }
                public void onNext(Object value) {
                    GrizzlyMessage retMessage = new GrizzlyMessage();
                    retMessage.setId(message.getId());
                    retMessage.setStatus(MessageStatus.Updating);
                    retMessage.setCommand(command.getName());
                    retMessage.setMetadata(message.getMetadata());
                    retMessage.setData(dealResult(command, value));
                    ctx.getConnection().write(retMessage);
                }
                public void onFailed(Throwable throwable) {
                    GrizzlyMessage retMessage = new GrizzlyMessage();
                    retMessage.setId(message.getId());
                    retMessage.setStatus(MessageStatus.Failed);
                    retMessage.setCommand(command.getName());
                    retMessage.setData(dealFailed(command, throwable));
                    retMessage.setMetadata(message.getMetadata());
                    ctx.getConnection().write(retMessage);
                }
                public void onCompleted() {
                    GrizzlyMessage retMessage = new GrizzlyMessage();
                    retMessage.setId(message.getId());
                    retMessage.setStatus(MessageStatus.Completed);
                    retMessage.setMetadata(message.getMetadata());
                    retMessage.setCommand(command.getName());
                    ctx.getConnection().write(retMessage);
                }
            };
            Object[] params;
            if(Stream.class.isAssignableFrom((Class<?>)paramTypes[0])){
                params = new Object[]{stream, ProtostuffUtils.deserialize(message.getData(), (Class<?>)paramTypes[1])};
            } else {
                params = new Object[]{ProtostuffUtils.deserialize(message.getData(), (Class<?>)paramTypes[0]), stream};
            }
            try {
              command.getMethod().invoke(command.getBean(), params);
            } catch (RuntimeException e){
                log.error(e.getMessage(), e);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                log.error(cause.getMessage(), cause);
            }
        } else {
            Object param = ProtostuffUtils.deserialize(message.getData(), (Class<?>)paramTypes[0]);
            Object result = null;
            try {
                result = command.getMethod().invoke(command.getBean(), new Object[]{param});
            } catch (RuntimeException e){
                log.error(e.getMessage(), e);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                log.error(cause.getMessage(), cause);
            }
            GrizzlyMessage retMessage = new GrizzlyMessage();
            if(command.getType() == MethodType.Default){// resp call(req)
                retMessage.setId(message.getId());
                retMessage.setStatus(MessageStatus.Completed);
                retMessage.setMetadata(message.getMetadata());
                retMessage.setData(dealResult(command, result));
                retMessage.setCommand(command.getName());
                ctx.getConnection().write(retMessage);
            }
            else if(command.getType() == MethodType.Future) {// future<resp> call(req)
                if(result == null){
                    retMessage.setId(message.getId());
                    retMessage.setStatus(MessageStatus.Completed);
                    retMessage.setMetadata(message.getMetadata());
                    retMessage.setCommand(command.getName());
                    ctx.getConnection().write(retMessage);
                } else {
                    ListenableFuture<Object> future = (ListenableFuture)result;
                    future.addCompleteHandler(new GrizzlyCompleteHandler(message, ctx.getConnection(), command));
                }
            }
        }
        return ctx.getStopAction();
    }

    public class GrizzlyCompleteHandler implements CompleteHandler<Object> {
        GrizzlyMessage message;
        Connection connection;
        ServerCommand command;
        public GrizzlyCompleteHandler(GrizzlyMessage message, Connection connection, ServerCommand command) {
            this.message = message;
            this.connection = connection;
            this.command = command;
        }


        public void onCompleted(Object result) {
            GrizzlyMessage retMessage = new GrizzlyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Completed);
            retMessage.setMetadata(message.getMetadata());
            retMessage.setData(dealResult(command, result));
            retMessage.setCommand(command.getName());
            connection.write(retMessage);
        }
        public void onCanceled() {
            GrizzlyMessage retMessage = new GrizzlyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Canceled);
            retMessage.setMetadata(message.getMetadata());
            retMessage.setCommand(command.getName());
            connection.write(retMessage);
        }
        public void onFailed(Throwable e) {
            GrizzlyMessage retMessage = new GrizzlyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Failed);
            retMessage.setMetadata(message.getMetadata());
            retMessage.setCommand(command.getName());
            retMessage.setData(dealFailed(command, e));
            connection.write(retMessage);
        }
    }

    private byte[] dealFailed(ServerCommand command, Throwable e) {
        return null;
    }

    private byte[] dealResult(ServerCommand command, Object result) {
        try {
            if(command.getType() == MethodType.Default) {
                return ProtostuffUtils.serialize((Class) command.getMethod().getReturnType(), result);
            } else if(command.getType() == MethodType.Future){
                ParameterizedType type = (ParameterizedType)command.getMethod().getGenericReturnType();
                Class clas = (Class)type.getActualTypeArguments()[0];
                return ProtostuffUtils.serialize(clas, result);
            } else if(command.getType() == MethodType.Stream){
                Type[] paramTypes = command.getMethod().getParameterTypes();
                if(Stream.class.isAssignableFrom((Class)paramTypes[0])){
                    return ProtostuffUtils.serialize((Class)paramTypes[1], result);
                } else {
                    return ProtostuffUtils.serialize((Class)paramTypes[0], result);
                }
            }
        } catch (Throwable e){
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
