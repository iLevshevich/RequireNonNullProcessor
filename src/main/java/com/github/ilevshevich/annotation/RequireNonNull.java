package com.github.ilevshevich.annotation;

import com.github.ilevshevich.misc.Inheritance;

import java.lang.annotation.*;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@Documented
public @interface RequireNonNull {
    Inheritance methodAnnotations() default Inheritance.ONLY_PROXY;
    Inheritance parameterAnnotations() default Inheritance.ONLY_PROXY;
    String namePostfix() default "";
}
