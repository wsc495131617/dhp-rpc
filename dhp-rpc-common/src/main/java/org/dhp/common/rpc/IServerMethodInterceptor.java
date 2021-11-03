package org.dhp.common.rpc;

import java.lang.reflect.Method;

/**
 * Rpc的方法拦截器
 */
public interface IServerMethodInterceptor {
    void before(Object o, Method method, Object[] objects) throws Throwable;
    void after(Object o, Method method, Object result) throws Throwable;
}
