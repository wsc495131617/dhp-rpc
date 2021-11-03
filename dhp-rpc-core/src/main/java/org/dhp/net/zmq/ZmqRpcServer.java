package org.dhp.net.zmq;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.rpc.IServerMethodInterceptor;
import org.dhp.common.utils.ProtostuffUtils;
import org.dhp.core.rpc.*;
import org.dhp.core.rpc.cmd.RpcCommand;
import org.dhp.core.rpc.cmd.ServerRpcCommand;
import org.dhp.net.BufferMessage;
import org.dhp.net.nio.MessageDecoder;
import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.jmxbase.GrizzlyJmxManager;
import org.glassfish.grizzly.memory.HeapBuffer;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

@Slf4j
public class ZmqRpcServer implements IRpcServer, Runnable {

    protected int port;

    ZMQ.Context context;
    ZMQ.Socket socket;

    RpcServerMethodManager methodManager;

    public ZmqRpcServer(int port) {
        this.port = port;
    }

    @Override
    public void start(RpcServerMethodManager methodManager) throws IOException {
        this.methodManager = methodManager;

        context = ZMQ.context(1);
        socket = context.socket(SocketType.REP);
        socket.bind("tcp://*:" + port);    //绑定端口

        //开启主循环
        Thread t = new Thread(this);
        t.setName("ZMQ_REP");
        t.start();

        //增加监控
        GrizzlyJmxManager jmxManager = GrizzlyJmxManager.instance();
        Object jmxMemoryManagerObject = MessageDecoder.memoryManager.getMonitoringConfig().createManagementObject();
        jmxManager.registerAtRoot(jmxMemoryManagerObject, "zmq_memory");
    }

    @Override
    public void running() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            byte[] body = socket.recv();  //获取request发送过来的数据
            //处理完就要立即返回
            BufferMessage message = new BufferMessage(HeapBuffer.wrap(body));
            ServerCommand command = methodManager.getCommand(message.getCommand());
            message = dealMessage(command, message);
            Buffer buffer = message.pack();
            buffer.toByteBuffer();
            socket.send(buffer.array(), buffer.arrayOffset(), buffer.limit(), 0);
        }
        socket.close();  //先关闭socket
        context.term();  //关闭当前的上下文
    }

    protected BufferMessage dealMessage(ServerCommand command, BufferMessage message) {
        Object result = null;
        Throwable throwable = null;
        if (command.getType() == MethodType.Command) { // RpcCommand
            RpcCommand rpcCommand = (RpcCommand) command.getBean();
            ServerRpcCommand serverRpcCommand = (ServerRpcCommand) command;
            result = rpcCommand.execute(ProtostuffUtils.deserialize(message.getData(), serverRpcCommand.getReqCls()));
        } else {
            Type[] paramTypes = command.getMethod().getParameterTypes();
            try {
                Object param = ProtostuffUtils.deserialize(message.getData(), (Class<?>) paramTypes[0]);
                Message.requestLatency.labels("beforeServerInvoke", message.getCommand(), message.getStatus().name()).observe(System.nanoTime() - message.getTs());
                result = invoke(command, new Object[]{param});
                Message.requestLatency.labels("serverInvoked", message.getCommand(), MessageStatus.Completed.name()).observe(System.nanoTime() - message.getTs());
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
        }
        if (command.getType() == MethodType.Default || command.getType() == MethodType.List || command.getType() == MethodType.Command) {// resp call(req)
            if (throwable != null) {
                message.setStatus(MessageStatus.Failed);
                message.setData(MethodDispatchUtils.dealFailed(command, throwable));
            } else {
                message.setStatus(MessageStatus.Completed);
                message.setData(MethodDispatchUtils.dealResult(command, result));
            }
            message.setMetadata(message.getMetadata());
            message.setCommand(command.getName());
            Message.requestLatency.labels("serverWrited", message.getCommand(), message.getStatus().name()).observe(System.nanoTime() - message.getTs());
            return message;
        } else if (command.getType() == MethodType.Future) {// future<resp> call(req)
            if (result == null) {
                message.setStatus(MessageStatus.Completed);
                message.setMetadata(message.getMetadata());
                message.setCommand(command.getName());
                Message.requestLatency.labels("serverWrited", message.getCommand(), message.getStatus().name()).observe(System.nanoTime() - message.getTs());
                return message;
            }
        }
        throw new RpcException(RpcErrorCode.UNSUPPORTED_COMMAND_TYPE);
    }
    private Object invoke(ServerCommand command, Object[] params) throws IllegalAccessException, InvocationTargetException {
        //处理拦截器
        if (command.getInterceptors() != null) {
            for (IServerMethodInterceptor interceptor : command.getInterceptors()) {
                try {
                    interceptor.before(command.getBean(), command.getMethod(), params);
                } catch (Throwable e) {
                    log.warn("interceptor.before exception: {}", e.getMessage(), e);
                }
            }
        }
        Object result = null;
        try {
            result = command.getMethod().invoke(command.getBean(), params);
            return result;
        } finally {
            //处理拦截器
            if (command.getInterceptors() != null) {
                for (IServerMethodInterceptor interceptor : command.getInterceptors()) {
                    try {
                        interceptor.after(command.getBean(), command.getMethod(), result);
                    } catch (Throwable e) {
                        log.warn("interceptor.after exception: {}", e.getMessage(), e);
                    }
                }
            }
        }
    }
}
