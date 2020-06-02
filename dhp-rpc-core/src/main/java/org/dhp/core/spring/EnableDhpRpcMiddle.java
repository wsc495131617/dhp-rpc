package org.dhp.core.spring;

import org.dhp.core.rpc.RpcChannelPool;
import org.dhp.core.rpc.RpcServerMethodManager;
import org.dhp.core.rpc.ServerStreamManager;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zhangcb
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({DhpClientRegister.class, DhpServerRegister.class,
        RpcChannelPool.class, RpcServer.class,
        RpcServerMethodManager.class, ServerStreamManager.class})
public @interface EnableDhpRpcMiddle {
    String[] basePackages() default {};
}
