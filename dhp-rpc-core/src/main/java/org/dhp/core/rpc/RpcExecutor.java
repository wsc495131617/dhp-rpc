package org.dhp.core.rpc;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.Stream;
import org.dhp.common.rpc.StreamFuture;
import org.dhp.common.utils.JacksonUtil;
import org.dhp.common.utils.ProtostuffUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class RpcExecutor {

    static RpcExecutorPool pool = new RpcExecutorPool(10);

    public static RpcExecutor create(ServerCommand command, Stream stream, Message message, Session session) throws InterruptedException {
        return pool.poll(command, stream, message, session);
    }

    Session session;
    Message message;
    ServerCommand command;
    Stream stream;

    public RpcExecutor() {

    }

    public RpcExecutor(ServerCommand command, Stream stream, Message message, Session session) {
        this.session = session;
        this.message = message;
        this.command = command;
        this.stream = stream;
    }

    public void execute() {
        if (command == null) {
            if (message.getCommand().equalsIgnoreCase("ping")) {
                message.setStatus(MessageStatus.Completed);
                message.setData((System.currentTimeMillis() + "").getBytes());
            } else {
                message.setStatus(MessageStatus.Failed);
                RpcFailedResponse rpcFailedResponse = new RpcFailedResponse();
                rpcFailedResponse.setClsName(RpcException.class.getName());
                RpcException rpcException = new RpcException(RpcErrorCode.COMMAND_NOT_IMPLEMENTED);
                rpcFailedResponse.setMessage(JacksonUtil.bean2Json(rpcException.getResponse()));
                message.setData(ProtostuffUtils.serialize(rpcFailedResponse));
                log.error("no command impl: {}", message.getCommand());
            }
            session.write(message);
            Message.requestLatency.labels("serverWrited", message.getCommand(), message.getStatus().name()).observe(System.nanoTime() - message.ts);
            return;
        }
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
                Message.requestLatency.labels("beforeServerStreamInvoke", message.getCommand(), message.getStatus().name()).observe(System.nanoTime() - message.ts);
                command.getMethod().invoke(command.getBean(), params);
                Message.requestLatency.labels("serverStreamInvoked", message.getCommand(), MessageStatus.Completed.name()).observe(System.nanoTime() - message.ts);
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
                Message.requestLatency.labels("beforeServerInvoke", message.getCommand(), message.getStatus().name()).observe(System.nanoTime() - message.ts);
                result = command.getMethod().invoke(command.getBean(), new Object[]{param});
                Message.requestLatency.labels("serverInvoked", message.getCommand(), MessageStatus.Completed.name()).observe(System.nanoTime() - message.ts);
            } catch (RuntimeException e) {
                log.error(e.getMessage(), e);
                throwable = e;
            } catch (IllegalAccessException e) {
                throwable = e;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause != null) {
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
                Message.requestLatency.labels("serverWrited", message.getCommand(), message.getStatus().name()).observe(System.nanoTime() - message.ts);
            } else if (command.getType() == MethodType.Future) {// future<resp> call(req)
                if (result == null) {
                    message.setStatus(MessageStatus.Completed);
                    message.setMetadata(message.getMetadata());
                    message.setCommand(command.getName());
                    session.write(message);
                    Message.requestLatency.labels("serverWrited", message.getCommand(), message.getStatus().name()).observe(System.nanoTime() - message.ts);
                } else {
                    StreamFuture<Object> future = (StreamFuture) result;
                    future.addStream(stream);
                    //加入到session管理里面，当session销毁，异步future就需要被cancel
                    session.addFuture(future);
                }
            }
        }
    }

    public void release() {
        this.message = null;
        this.session = null;
        this.command = null;
        this.stream = null;
        pool.offer(this);
    }

    static class RpcExecutorPool {

        final BlockingQueue<RpcExecutor> cache;

        final int size;

        public RpcExecutorPool(int size) {
            this.cache = new LinkedBlockingQueue<>(size);
            //初始化
            for (int i = 0; i < size; i++) {
                this.cache.add(new RpcExecutor());
            }
            this.size = size;
        }

        public RpcExecutor poll(ServerCommand command, Stream stream, Message message, Session session) throws InterruptedException {
            RpcExecutor executor = cache.poll(100, TimeUnit.MILLISECONDS);
            executor.init(command, stream, message, session);
            return executor;
        }

        public void offer(RpcExecutor ex) {
            cache.offer(ex);
        }
    }

    private void init(ServerCommand command, Stream stream, Message message, Session session) {
        this.command = command;
        this.stream = stream;
        this.message = message;
        this.session = session;
    }

}
