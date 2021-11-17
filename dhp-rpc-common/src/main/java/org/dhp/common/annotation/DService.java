package org.dhp.common.annotation;

import java.lang.annotation.*;

/**
 * 注解interface
 * node写死服务的服务名称
 * prop通过属性找到对应node的值，一般通过从方法体的Req里面获取
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DService {
    String node() default "default";
    String prop() default "";
}
