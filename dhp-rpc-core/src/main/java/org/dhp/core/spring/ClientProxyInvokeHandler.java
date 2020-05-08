package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.annotation.DMethod;
import org.dhp.common.annotation.DService;
import org.dhp.core.rpc.*;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.reflect.Method;
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
        MethodType methodType;

        //入参为1个
        if (args.length == 1) {
            if(returnType instanceof Future){
                methodType = MethodType.Future;
            } else {
                methodType = MethodType.Void;
            }
            argBody = encodeArgument(paramTypes[0], args[0]);
        } else if (args.length == 2) {//如果入参是2个，那么就说明，其中一个是入参对象，另外一个是Stream流对象
            if(paramTypes[0] instanceof Stream){
                argBody = encodeArgument(paramTypes[1], args[1]);
            } else {
                argBody = encodeArgument(paramTypes[0], args[0]);
            }
            methodType = MethodType.Stream;
        } else {
            throw new RpcException(ErrorCode.ILLEGAL_PARAMETER_DEFINITION);
        }
        //发送
        Long messageId = sendMessage(command, argBody);
        if(messageId == null){
            throw new RpcException(ErrorCode.SEND_MESSAGE_FAILED);
        }

        //如果返回是一个Future，那么说明方法是一个异步方法
        if (methodType == MethodType.Stream) {

        } else {//同步方法返回

        }
        return null;
    }

    protected byte[] encodeArgument(Type type, Object arg) {
        return null;
    }

    /**
     * 发送消息，并返回消息编号
     * @param command
     * @param argBody
     * @return
     */
    protected Long sendMessage(Command command, byte[] argBody) {
        Node node = getNode(command);
        if(node == null){
            throw new RpcException(ErrorCode.NODE_NOT_FOUND);
        }
        log.info("send message to node {}", node);
        return 0l;
    }


    BeanFactory beanFactory;

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
