package org.dhp.common.annotation;

import java.lang.annotation.*;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DMethod {

    String command() default "";

    long timeout() default -1;

    String directProp() default "";
}
