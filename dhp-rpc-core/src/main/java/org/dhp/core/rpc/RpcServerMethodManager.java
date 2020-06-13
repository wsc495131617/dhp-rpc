package org.dhp.core.rpc;

import lombok.extern.slf4j.Slf4j;
import org.dhp.common.annotation.DMethod;
import org.dhp.common.annotation.DService;
import org.dhp.common.rpc.StreamFuture;
import org.dhp.core.spring.FrameworkException;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server method manager, store all method
 * @author zhangcb
 */
@Slf4j
public class RpcServerMethodManager implements IMethodManager{

    Map<String, ServerCommand> commands = new ConcurrentHashMap<>();

    public void addServiceBean(Object bean, Class<?> cls) {
        log.info("Add Service {}", cls.getName());
        Method[] methods = cls.getDeclaredMethods();
        DService dService = cls.getAnnotation(DService.class);
        for (Method method : methods) {
            addMethod(method, bean, cls);
        }
    }

    protected void addMethod(Method method, Object bean, Class<?> cls) {
        log.info("Add method {}", method);
        ServerCommand command = new ServerCommand();
        command.setMethod(method);
        command.setCls(cls);
        command.setBean(bean);
        String methodName = method.getName();
        String className = method.getDeclaringClass().getName();

        String commandName = className + ":" + methodName;
        DMethod dm = method.getAnnotation(DMethod.class);
        //if defined Dmethod annotation, use dmethod to send
        if (dm != null && !StringUtils.isEmpty(dm.command())) {
            commandName = dm.command();
        }
        command.setName(commandName);
        commands.putIfAbsent(commandName, command);

        Type[] args = method.getParameterTypes();
        Type returnType = method.getGenericReturnType();
        MethodType methodType;

        //入参为1个
        if (args.length == 1) {
            //一定要是ListenableFuture 或者是 List 多结果集
            if (returnType instanceof ParameterizedType) {
                if(List.class.isAssignableFrom((Class)((ParameterizedType) returnType).getRawType())){
                    methodType = MethodType.List;
                }
                else if (StreamFuture.class.isAssignableFrom((Class<?>) ((ParameterizedType) returnType).getRawType())) {
                    methodType = MethodType.Future;
                } else {
                    throw new FrameworkException("Method ParameterizedType Return must be ListenableFuture");
                }
            } else {
                methodType = MethodType.Default;
            }

        } else if (args.length == 2) {//如果入参是2个，那么就说明，其中一个是入参对象，另外一个是Stream流对象
            methodType = MethodType.Stream;
        } else {
            throw new RpcException(RpcErrorCode.ILLEGAL_PARAMETER_DEFINITION);
        }
        command.setType(methodType);

    }

    public ServerCommand getCommand(String command) {
        return commands.get(command);
    }
}
