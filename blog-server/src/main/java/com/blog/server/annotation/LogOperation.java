package com.blog.server.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {
    /**
     * The type of operation, e.g., "LOGIN", "REGISTER", "DELETE"
     */
    String type() default "";
    
    /**
     * Description of the operation
     */
    String description() default "";
}
