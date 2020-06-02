package org.dhp.core.rpc;

import lombok.Data;

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
}
