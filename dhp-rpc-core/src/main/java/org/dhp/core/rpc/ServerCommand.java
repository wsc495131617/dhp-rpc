package org.dhp.core.rpc;

import lombok.Data;
import org.dhp.common.rpc.IServerMethodInterceptor;

import java.lang.reflect.Method;

/**
 * @author zhangcb
 */
@Data
public class ServerCommand {
    Method method;
    String name;
    Class<?> cls;
    Object bean;
    MethodType type;
    IServerMethodInterceptor[] interceptors;
}
