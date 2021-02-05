package org.chzcb.zk;

import java.lang.annotation.*;

/**
 * 注解ZKLock，通过name进行全局业务的唯一性保障
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
