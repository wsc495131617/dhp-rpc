package org.chzcb.quartz;


import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QuartzScheduled {
    String value() default "";
    String name() default "";
    Class<?> cls() default DefaultQuartzJobBean.class;
}
