package org.dhp.core.spring;

import org.dhp.core.rpc.RpcServerMethodManager;
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
        RpcServer.class,
        RpcServerMethodManager.class})
public @interface EnableDhpRpcMiddle {
    String[] basePackages() default {};
}
