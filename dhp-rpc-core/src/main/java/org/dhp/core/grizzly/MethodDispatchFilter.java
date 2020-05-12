package org.dhp.core.grizzly;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.common.rpc.StreamFuture;
import org.dhp.common.utils.ProtostuffUtils;
import org.dhp.core.rpc.*;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

@Slf4j
public class MethodDispatchFilter extends BaseFilter {
    RpcServerMethodManager methodManager;
    
    GrizzlySessionManager sessionManager;
    
    public MethodDispatchFilter(RpcServerMethodManager methodManager, GrizzlySessionManager sessionManager) {
        this.methodManager = methodManager;
        this.sessionManager = sessionManager;
    }
    
    @Override
    public NextAction handleClose(FilterChainContext ctx) throws IOException {
        sessionManager.destorySession(ctx.getConnection());
        return super.handleClose(ctx);
    }
    
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        GrizzlyMessage message = ctx.getMessage();
        Session session = sessionManager.getSession(ctx.getConnection());
        if(!session.isRegister()){
            if(message.getCommand().equalsIgnoreCase("register")){
               session.setId(ProtostuffUtils.deserialize(message.getData(), Long.class));
                sessionManager.register(session);
            } else {
                log.warn("收到未注册消息，丢弃: {}", message);
            }
            return ctx.getStopAction();
        }
        ServerCommand command = methodManager.getCommand(message.getCommand());
        if(command == null) {
            if(message.getCommand().equalsIgnoreCase("ping")){
                GrizzlyMessage retMessage = new GrizzlyMessage();
                retMessage.setId(message.getId());
                retMessage.setStatus(MessageStatus.Completed);
                retMessage.setCommand(message.getCommand());
                retMessage.setData((System.currentTimeMillis()+"").getBytes());
                ctx.getConnection().write(retMessage);
            } else {
                GrizzlyMessage retMessage = new GrizzlyMessage();
                retMessage.setId(message.getId());
                retMessage.setStatus(MessageStatus.Failed);
                retMessage.setCommand(message.getCommand());
                retMessage.setData("no command".getBytes());
                ctx.getConnection().write(retMessage);
            }
        } else {
            Type[] paramTypes = command.getMethod().getParameterTypes();
            Stream stream = new GrizzlyStream(session.getId(), command, message);
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
                try {
                    result = command.getMethod().invoke(command.getBean(), new Object[]{param});
                } catch (RuntimeException e) {
                    log.error(e.getMessage(), e);
                } catch (IllegalAccessException e) {
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    log.error(cause.getMessage(), cause);
                }
                GrizzlyMessage retMessage = new GrizzlyMessage();
                if (command.getType() == MethodType.Default) {// resp call(req)
                    retMessage.setId(message.getId());
                    retMessage.setStatus(MessageStatus.Completed);
                    retMessage.setMetadata(message.getMetadata());
                    retMessage.setData(MethodDispatchUtils.dealResult(command, result));
                    retMessage.setCommand(command.getName());
                    ctx.getConnection().write(retMessage);
                } else if (command.getType() == MethodType.Future) {// future<resp> call(req)
                    if (result == null) {
                        retMessage.setId(message.getId());
                        retMessage.setStatus(MessageStatus.Completed);
                        retMessage.setMetadata(message.getMetadata());
                        retMessage.setCommand(command.getName());
                        ctx.getConnection().write(retMessage);
                    } else {
                        StreamFuture<Object> future = (StreamFuture) result;
                        future.addStream(stream);
                    }
                }
            }
        }
        return ctx.getStopAction();
    }

    class GrizzlyStream<T> implements Stream<T> {

        Long sessionId;
        ServerCommand command;
        Message message;

        public GrizzlyStream(Long sessionId, ServerCommand command, Message message){
            this.sessionId = sessionId;
            this.command = command;
            this.message = message;
        }

        public void onCanceled() {
            GrizzlyMessage retMessage = new GrizzlyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Canceled);
            retMessage.setMetadata(message.getMetadata());
            retMessage.setCommand(command.getName());
            sessionManager.getSessionById(sessionId).write(retMessage);
        }

        public void onNext(Object value) {
            GrizzlyMessage retMessage = new GrizzlyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Updating);
            retMessage.setCommand(command.getName());
            retMessage.setMetadata(message.getMetadata());
            retMessage.setData(MethodDispatchUtils.dealResult(command, value));
            Session session = sessionManager.getSessionById(sessionId);
            if(session != null)
                session.write(retMessage);
        }

        public void onFailed(Throwable throwable) {
            GrizzlyMessage retMessage = new GrizzlyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Failed);
            retMessage.setCommand(command.getName());
            retMessage.setData(MethodDispatchUtils.dealFailed(command, throwable));
            retMessage.setMetadata(message.getMetadata());
            sessionManager.getSessionById(sessionId).write(retMessage);
        }

        public void onCompleted() {
        }
    }

}
