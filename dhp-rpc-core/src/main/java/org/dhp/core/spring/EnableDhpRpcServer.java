package org.dhp.core.spring;

import org.dhp.core.rpc.RpcServerMethodManager;
import org.dhp.core.rpc.ServerStreamManager;
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
@Import({DhpServerRegister.class, RpcServer.class, RpcServerMethodManager.class, ServerStreamManager.class})
public @interface EnableDhpRpcServer {
}
