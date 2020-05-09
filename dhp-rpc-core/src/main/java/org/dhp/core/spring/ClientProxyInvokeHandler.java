package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.annotation.DMethod;
import org.dhp.common.annotation.DService;
import org.dhp.common.utils.ProtostuffUtils;
import org.dhp.core.rpc.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 对于invoke的方法，应该需要统一标准，所有入参都应该继承RpcRequest或者增加Stream流入参，用于多个结果的返回，所有出参都应该集成RpcResponse或者是FutureResponse
 */
@Slf4j
public class ClientProxyInvokeHandler implements InvocationHandler, ImportBeanDefinitionRegistrar, BeanFactoryAware {

    @Resource
    DhpProperties dhpProperties;

    @Resource
    RpcChannelPool channelPool;

    protected Set<Method> excludeMethods = ConcurrentHashMap.newKeySet();

    protected Map<Method, Command> cacheCommands = new ConcurrentHashMap<>();

    protected ExecutorService pool = Executors.newCachedThreadPool(new ThreadFactory() {
        private int i = 1;
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("ClientProxy_"+(i++));
            t.setDaemon(true);
            return t;
        }
    });

    protected boolean isExcludeMethod(Method method) {
        if (excludeMethods.contains(method)) {
            return true;
        }
        return false;
    }

    protected Command getCommand(Method method) {
        if(cacheCommands.containsKey(method)){
            return cacheCommands.get(method);
        }
        Command command = new Command();
        command.setCls(method.getDeclaringClass());
        command.setMethod(method);

        DService service = command.getCls().getAnnotation(DService.class);
        command.setNodeName(service.node());

        String methodName = method.getName();
        String className = method.getDeclaringClass().getName();

        String commandName = className+":"+methodName;
        DMethod dm = method.getAnnotation(DMethod.class);
        //if defined Dmethod annotation, use dmethod to send
        if (dm != null && !StringUtils.isEmpty(dm.command())) {
            commandName = dm.command();
        }
        command.setName(commandName);
        cacheCommands.put(method, command);
        return command;
    }

    protected Node getNode(Command command) {
        if(command.getNodeName() != null){
            for(Node node : dhpProperties.getNodes()){
                //如果
                if(node.getName().equals(command.getNodeName())){
                    return node;
                }
            }
        }
        return null;
    }

    public Object invoke(Object o, Method method, Object[] args) throws Throwable {
        if (isExcludeMethod(method)) {
            return method.invoke(o, args);
        }
        Command command = getCommand(method);
        if (command == null) {
            throw new RpcException(ErrorCode.COMMAND_NOT_FOUND);
        }

        Type returnType = method.getReturnType();
        Type[] paramTypes = method.getParameterTypes();

        byte[] argBody;
        MethodType methodType = MethodType.Default;
        Stream argStream = null;
        FutureImpl future = null;
        //入参为1个
        if (args.length == 1) {
            future = new FutureImpl();
            if(ListenableFuture.class.isAssignableFrom((Class)returnType)){
                methodType = MethodType.Future;
            } else {
                methodType = MethodType.Default;
            }
            argBody = ProtostuffUtils.serialize((Class)paramTypes[0], args[0]);
        } else if (args.length == 2) {//如果入参是2个，那么就说明，其中一个是入参对象，另外一个是Stream流对象
            if(paramTypes[0] instanceof Stream){
                argBody = ProtostuffUtils.serialize((Class)paramTypes[1], args[1]);
                argStream = (Stream) args[0];
            } else {
                argBody = ProtostuffUtils.serialize((Class)paramTypes[0], args[0]);
                argStream = (Stream) args[1];
            }
            methodType = MethodType.Stream;
        } else {
            throw new RpcException(ErrorCode.ILLEGAL_PARAMETER_DEFINITION);
        }
        MethodType finalMethodType = methodType;
        Stream finalArgStream = argStream;
        FutureImpl finalFuture = future;
        MethodType finalMethodType1 = methodType;
        Stream<byte[]> stream = new Stream<byte[]>() {
            public void onCanceled() {
                if (finalMethodType == MethodType.Stream) {
                    finalArgStream.onCanceled();
                } else {
                    finalFuture.cancel(false);
                }
            }
            public void onNext(byte[] value) {
                Object ret = dealResult(finalMethodType1, method, value);
                if (finalMethodType == MethodType.Stream) {
                    finalArgStream.onNext(ret);
                } else {
                    finalFuture.result(ret);
                }
            }
            public void onFailed(Throwable throwable) {
                if (finalMethodType == MethodType.Stream) {
                    finalArgStream.onFailed(throwable);
                } else {
                    finalFuture.result(throwable);
                }
            }
            public void onCompleted() {
                if (finalMethodType == MethodType.Stream) {
                    finalArgStream.onCompleted();
                } else {
                    finalFuture.result(null);
                }
            }
        };
        //发送
        Integer messageId = sendMessage(command, argBody, stream);
        if(messageId == null){
            throw new RpcException(ErrorCode.SEND_MESSAGE_FAILED);
        }
        if (finalMethodType == MethodType.Stream) {
            return null;
        } else if (finalMethodType == MethodType.Future) {
            return future;
        } else if(future != null)
            return future.get();
        else
            return null;
    }
    private Object dealResult(MethodType methodType, Method method, byte[] result) {
        try {
            if(methodType == MethodType.Default) {
                return ProtostuffUtils.deserialize(result, (Class) method.getReturnType());
            } else if(methodType == MethodType.Future){
                ParameterizedType type = (ParameterizedType) method.getGenericReturnType();
                Class clas = (Class)type.getActualTypeArguments()[0];
                return ProtostuffUtils.deserialize(result, clas);
            } else if(methodType == MethodType.Stream){
                Type[] paramTypes = method.getParameterTypes();
                if(Stream.class.isAssignableFrom((Class)paramTypes[0])){
                    return ProtostuffUtils.deserialize(result, (Class)paramTypes[1]);
                } else {
                    return ProtostuffUtils.deserialize(result, (Class)paramTypes[0]);
                }
            }
        } catch (Throwable e){
            log.error(e.getMessage(), e);
        }
        return null;
    }
    /**
     * 发送消息，并返回消息编号
     * @param command
     * @param argBody
     * @return
     */
    protected Integer sendMessage(Command command, byte[] argBody, Stream<byte[]> stream) {
        Node node = getNode(command);
        if(node == null){
            throw new RpcException(ErrorCode.NODE_NOT_FOUND);
        }
        RpcChannel channel = channelPool.getChannel(node);
        channel.write(command.getName(), argBody, stream);

        return 0;
    }


    BeanFactory beanFactory;

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
