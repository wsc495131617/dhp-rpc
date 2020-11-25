package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.common.rpc.StreamFuture;
import org.dhp.common.utils.ProtostuffUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

@Slf4j
public class RpcExecutor {

    final Session session;
    final Message message;
    final ServerCommand command;
    final Stream stream;

    public RpcExecutor(ServerCommand command, Stream stream, Message message, Session session) {
        this.session = session;
        this.message = message;
        this.command = command;
        this.stream = stream;
    }

    public void execute() {
        Type[] paramTypes = command.getMethod().getParameterTypes();
        if (command.getType() == MethodType.Stream) {// call(req, stream<resp>)
            Object[] params;
            if (Stream.class.isAssignableFrom((Class<?>) paramTypes[0])) {
                params = new Object[]{stream, ProtostuffUtils.deserialize(message.getData(), (Class<?>) paramTypes[1])};
            } else {
                params = new Object[]{ProtostuffUtils.deserialize(message.getData(), (Class<?>) paramTypes[0]), stream};
            }
            try {
                //这里的stream会受到服务端自己管理，因此当session关闭的时候，考虑集群，不能把客户端的stream留在本地，需要移除，
                // 因此需要加入到session管理，session销毁，就应该关闭stream，让客户端自己重新发起stream的请求
                command.getMethod().invoke(command.getBean(), params);
                session.addStream(stream);
            } catch (RuntimeException e) {
                log.error(e.getMessage(), e);
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                log.error(cause.getMessage(), cause);
            }
        } else {
            Object result = null;
            Throwable throwable = null;
            try {
                Object param = ProtostuffUtils.deserialize(message.getData(), (Class<?>) paramTypes[0]);
                result = command.getMethod().invoke(command.getBean(), new Object[]{param});
            } catch (RuntimeException e) {
                log.error(e.getMessage(), e);
                throwable = e;
            } catch (IllegalAccessException e) {
                throwable = e;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if(cause != null) {
                    log.error(cause.getMessage(), cause);
                }
                throwable = cause;
            }
            if (command.getType() == MethodType.Default || command.getType() == MethodType.List) {// resp call(req)
                if (throwable != null) {
                    message.setStatus(MessageStatus.Failed);
                    message.setData(MethodDispatchUtils.dealFailed(command, throwable));
                } else {
                    message.setStatus(MessageStatus.Completed);
                    message.setData(MethodDispatchUtils.dealResult(command, result));
                }
                message.setMetadata(message.getMetadata());
                message.setCommand(command.getName());
                session.write(message);
            } else if (command.getType() == MethodType.Future) {// future<resp> call(req)
                if (result == null) {
                    message.setStatus(MessageStatus.Completed);
                    message.setMetadata(message.getMetadata());
                    message.setCommand(command.getName());
                    session.write(message);
                } else {
                    StreamFuture<Object> future = (StreamFuture) result;
                    future.addStream(stream);
                    //加入到session管理里面，当session销毁，异步future就需要被cancel
                    session.addFuture(future);

                }
            }
        }
    }
}
