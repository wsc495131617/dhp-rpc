package org.dhp.core.spring;

import org.dhp.core.rpc.RpcChannelPool;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import({DhpClientRegister.class, ClientProxyInvokeHandler.class, RpcChannelPool.class})
public @interface EnableDhpRpcClient {
    String[] basePackages() default {};
}
