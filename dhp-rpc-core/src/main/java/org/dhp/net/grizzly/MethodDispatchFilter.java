package org.dhp.net.grizzly;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.common.utils.ProtostuffUtils;
import org.dhp.core.rpc.*;
import org.glassfish.grizzly.filterchain.BaseFilter;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;

import java.io.IOException;

/**
 * @author zhangcb
 */
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
    
    @Override
    public NextAction handleConnect(FilterChainContext ctx) throws IOException {
        if (sessionManager.isClosing()) {
            ctx.getConnection().close();
            return ctx.getStopAction();
        }
        return super.handleConnect(ctx);
    }
    
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        GrizzlyMessage message = ctx.getMessage();
        Session session = sessionManager.getSession(ctx.getConnection());
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
                log.warn("收到未注册消息，丢弃: {}, 并关闭连接: {}", message, ctx.getConnection());
                ctx.getConnection().close();
            }
            return ctx.getStopAction();
        }
        ServerCommand command = methodManager.getCommand(message.getCommand());
        Stream stream = new GrizzlyStream(session.getId(), command, message);
        Workers.getWorker(message).execute(command, stream, message, session);
        return ctx.getStopAction();
    }

    class GrizzlyStream<T> implements Stream<T> {
        
        Long sessionId;
        ServerCommand command;
        Message message;
        
        public GrizzlyStream(Long sessionId, ServerCommand command, Message message) {
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
            Session session = sessionManager.getSessionById(sessionId);
            if (session != null)
                session.write(retMessage);
        }
        
        public void onNext(Object value) {
            GrizzlyMessage retMessage = new GrizzlyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Updating);
            retMessage.setCommand(command.getName());
            retMessage.setMetadata(message.getMetadata());
            retMessage.setData(MethodDispatchUtils.dealResult(command, value));
            Session session = sessionManager.getSessionById(sessionId);
            if (session != null)
                session.write(retMessage);
        }
        
        public void onFailed(Throwable throwable) {
            GrizzlyMessage retMessage = new GrizzlyMessage();
            retMessage.setId(message.getId());
            retMessage.setStatus(MessageStatus.Failed);
            retMessage.setCommand(command.getName());
            retMessage.setData(MethodDispatchUtils.dealFailed(command, throwable));
            retMessage.setMetadata(message.getMetadata());
            Session session = sessionManager.getSessionById(sessionId);
            if (session != null)
                session.write(retMessage);
        }
        
        public void onCompleted() {
        }
    }
    
}
