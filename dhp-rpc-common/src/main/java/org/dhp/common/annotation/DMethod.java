package org.dhp.common.annotation;

import org.dhp.common.rpc.IServerMethodInterceptor;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DMethod {

    String command() default "";

    long timeout() default -1;

    /**
     * 重试次数 需要慎重使用，只有支持幂等以及查询类方法才能通用
     */
    long retry() default -1;

    /**
     * 是否是广播，广播的话，只需要有一个下游节点返回就可以
     * @return
     */
    boolean broadcast() default false;

    /**
     * 拦截器
     * @return
     */
    Class<? extends IServerMethodInterceptor>[] intercepts() default {};
}
