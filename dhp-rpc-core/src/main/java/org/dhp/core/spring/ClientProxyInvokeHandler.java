package org.dhp.core.spring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.proxy.InvocationHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * 对于invoke的方法，应该需要统一标准，所有入参都应该继承RpcRequest或者增加Stream流入参，用于多个结果的返回，所有出参都应该集成RpcResponse或者是FutureResponse
 */
@Slf4j
public class ClientProxyInvokeHandler implements InvocationHandler {

    protected Set<Method> excludeMethods = ConcurrentHashMap.newKeySet();

    protected boolean isExcludeMethod(Method method){
        if(excludeMethods.contains(method)){
            return true;
        }
        return false;
    }

    public Object invoke(Object o, Method method, Object[] args) throws Throwable {
        if(method.getName().equalsIgnoreCase("toString")){
            return true;
        }
        if(isExcludeMethod(method)){
            return method.invoke(o, args);
        }
        log.info("invoke {}", method);

        Type returnType = method.getGenericReturnType();
        //入参为1个
        if(args.length == 1){
            //如果返回是一个Future，那么说明方法是一个异步方法
            if(returnType instanceof Future){

            } else {//同步方法返回

            }
        } else if(args.length == 2){//如果入参是2个，那么就说明，其中一个是入参对象，另外一个是Stream流对象

        }
        return null;
    }
}
