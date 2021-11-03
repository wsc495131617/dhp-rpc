package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.annotation.DMethod;
import org.dhp.common.annotation.DService;
import org.dhp.common.rpc.IServerMethodInterceptor;
import org.dhp.common.rpc.StreamFuture;
import org.dhp.core.rpc.cmd.PingCommand;
import org.dhp.core.rpc.cmd.RpcCommand;
import org.dhp.core.rpc.cmd.ServerRpcCommand;
import org.dhp.core.spring.FrameworkException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Server method org.chzcb.strategy.manager, store all method
 * @author zhangcb
 */
@Slf4j
public class RpcServerMethodManager implements IMethodManager, ApplicationListener<ApplicationReadyEvent> {

    @Resource
    ApplicationContext applicationContext;

    Map<String, ServerCommand> commands = new ConcurrentHashMap<>();

    Set<String> nodeNames = new HashSet<>();

    public void initBasicCommand() {
        PingCommand pingCommand = new PingCommand();
        addCommand(pingCommand);
    }

    public void addServiceBean(Object bean, Class<?> cls) {
        Method[] methods = cls.getDeclaredMethods();
        DService dService = cls.getAnnotation(DService.class);
        String nodeName = dService.node();
        nodeNames.add(nodeName);
        log.info("Add Service={}, node={}, bean={}", cls.getName(), nodeName, bean);
        for (Method method : methods) {
            addMethod(method, bean, cls);
        }
    }

    public void addCommand(RpcCommand rpcCommand) {
        log.info("Add command: {}", rpcCommand.name());
        ServerRpcCommand command = new ServerRpcCommand();
        command.setBean(rpcCommand);
        command.setType(MethodType.Command);
        command.setName(rpcCommand.name());
        commands.put(command.getName(), command);

        //属于
        ParameterizedType parameterizedType = (ParameterizedType) rpcCommand.getClass().getGenericInterfaces()[0];
        Type[] types = parameterizedType.getActualTypeArguments();
        if(types.length == 2) {
            command.setReqCls((Class<?>) types[0]);
            command.setRespCls((Class<?>) types[1]);
        }
    }

    /**
     * 增加服务端的方法处理，这时候需要增加拦截器解析，如果直接使用AOP，性能还是很受影响，因此底层考虑实现拦截器组件
     * @param method
     * @param bean
     * @param cls
     */
    protected void addMethod(Method method, Object bean, Class<?> cls) {
        log.info("Add method: {}", method);
        ServerCommand command = new ServerCommand();
        command.setMethod(method);
        command.setCls(cls);
        command.setBean(bean);
        String methodName = method.getName();
        String className = method.getDeclaringClass().getName();

        String commandName = org.dhp.common.utils.StringUtils.simplePackage(className + ":" + methodName);
        DMethod dm = method.getAnnotation(DMethod.class);
        //if defined Dmethod annotation, use dmethod to send
        if (dm != null && !StringUtils.isEmpty(dm.command())) {
            commandName = dm.command();
        }
        IServerMethodInterceptor[] interceptors = null;
        //if defined interceptor
        if (dm != null && dm.intercepts().length>0) {
            interceptors = new IServerMethodInterceptor[dm.intercepts().length];
            for(int i = 0;i<dm.intercepts().length;i++) {
                Class<? extends IServerMethodInterceptor> intereptorCls = dm.intercepts()[i];
                try {
                    IServerMethodInterceptor interceptorBean = applicationContext.getBean(intereptorCls);
                    interceptors[i] = interceptorBean;
                } catch (Exception e) {
                    throw new RpcException(RpcErrorCode.COMMAND_INTERCEPTOR_NOT_FOUND);
                }
            }
        }
        command.setName(commandName);
        commands.putIfAbsent(commandName, command);

        Type[] args = method.getParameterTypes();
        Type returnType = method.getGenericReturnType();
        MethodType methodType;

        //1 argument
        if (args.length == 1) {
            //must be StreamFuture or List result
            if (returnType instanceof ParameterizedType) {
                if(List.class.isAssignableFrom((Class)((ParameterizedType) returnType).getRawType())){
                    methodType = MethodType.List;
                }
                else if (StreamFuture.class.isAssignableFrom((Class<?>) ((ParameterizedType) returnType).getRawType())) {
                    methodType = MethodType.Future;
                } else {
                    throw new FrameworkException("Method ParameterizedType Return must be StreamFuture");
                }
            } else {
                methodType = MethodType.Default;
            }

        } else if (args.length == 2) {//如果入参是2个，那么就说明，其中一个是入参对象，另外一个是Stream流对象
            methodType = MethodType.Stream;
        } else {// Unsupport larger then 2 arguments
            throw new RpcException(RpcErrorCode.ILLEGAL_PARAMETER_DEFINITION);
        }
        command.setType(methodType);

    }

    public ServerCommand getCommand(String command) {
        try {
            readyLock.await();
        } catch (InterruptedException e) {
            log.error("interrupted {}", e.getMessage(), e);
        }
        return commands.get(command);
    }

    protected CountDownLatch readyLock = new CountDownLatch(1);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        readyLock.countDown();
    }
}
