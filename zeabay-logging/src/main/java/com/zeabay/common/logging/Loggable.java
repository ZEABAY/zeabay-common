package com.zeabay.common.logging;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or class for automatic entry/exit logging via {@link LoggingAspect}.
 *
 * <p>When applied at the class level, all public methods in the class are logged. Method-level
 * annotations override class-level settings.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Loggable {

  /** Whether method arguments should be included in log output. */
  boolean logArgs() default true;

  /** Whether the return value should be included in log output. */
  boolean logResult() default true;
}
