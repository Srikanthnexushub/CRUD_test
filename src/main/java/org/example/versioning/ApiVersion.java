package org.example.versioning;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify API version for controllers and methods.
 * Enables URL-based versioning (e.g., /api/v1/users, /api/v2/users).
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {
    /**
     * The version number (e.g., 1, 2, 3).
     */
    int value();

    /**
     * Whether this version is deprecated.
     */
    boolean deprecated() default false;

    /**
     * Deprecation message (optional).
     */
    String deprecationMessage() default "";
}
