package org.chzcb.zk;

import java.lang.annotation.*;

/**
 * 注解ZKLock
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ZKLocked {
    /**
     * 注解某个方法，确保ZK分布式锁生效
     * @return
     */
    String name() default "";
}
