package com.blog.server.annotation;

import java.lang.annotation.*;

/**
 * Global API rate limiting annotation bridging Redis bucket architectures.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /**
     * Optional custom key. If blank, defaults to method name.
     */
    String key() default "";
    
    /**
     * Peak maximum requests allowed within the defined sliding window.
     */
    int maxRequests() default 50;
    
    /**
     * Fixed tracking window in seconds.
     */
    int windowSeconds() default 10;
}
