package com.zeabay.common.logging;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.zeabay.common.constant.ZeabayConstants;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * AOP aspect that provides structured entry/exit logging for methods annotated with {@link
 * Loggable}.
 *
 * <p>Supports synchronous return types, {@link Mono}, and {@link Flux}. Log lines include trace ID,
 * client IP, authenticated user, HTTP method, and request path from the Reactor context or MDC.
 */
@Slf4j
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class LoggingAspect {

  private static final String HIDDEN_VALUE = "[HIDDEN]";
  private static final int MAX_ARGS_LENGTH = 500;

  @Around("@annotation(loggable) || @within(loggable)")
  public Object logAround(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    if (loggable == null) {
      loggable = joinPoint.getTarget().getClass().getAnnotation(Loggable.class);
    }

    String methodId = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    boolean logArgs = loggable != null && loggable.logArgs();
    boolean logRes = loggable != null && loggable.logResult();
    String args = logArgs ? safeArgsToString(joinPoint.getArgs()) : HIDDEN_VALUE;

    Class<?> returnType = signature.getReturnType();
    boolean reactiveReturn =
        Mono.class.isAssignableFrom(returnType) || Flux.class.isAssignableFrom(returnType);

    if (!reactiveReturn) {
      logEntry(null, methodId, args);
    }

    Object result;
    long assemblyTime = System.currentTimeMillis();

    try {
      result = joinPoint.proceed();
    } catch (Throwable ex) {
      if (reactiveReturn) {
        logEntry(null, methodId, args);
      }
      log.error(
          "{} <== [{}] failed after {}ms. Reason: {}",
          getLogPrefix(null),
          methodId,
          (System.currentTimeMillis() - assemblyTime),
          ex.getMessage());
      throw ex;
    }

    return switch (result) {
      case null -> {
        if (reactiveReturn) {
          logEntry(null, methodId, args);
        }
        logSyncExit(methodId, null, assemblyTime, logRes);
        yield null;
      }
      case Mono<?> mono ->
          mono.transformDeferredContextual(
              (orig, ctx) -> {
                long subStart = System.currentTimeMillis();
                logEntry(ctx, methodId, args);
                return logMonoResult(orig, methodId, subStart, logRes, ctx);
              });

      case Flux<?> flux ->
          flux.transformDeferredContextual(
              (orig, ctx) -> {
                long subStart = System.currentTimeMillis();
                logEntry(ctx, methodId, args);
                return logFluxResult(orig, methodId, subStart, logRes, ctx);
              });

      default -> {
        if (reactiveReturn) {
          logEntry(null, methodId, args);
        }
        logSyncExit(methodId, result, assemblyTime, logRes);
        yield result;
      }
    };
  }

  private static String safeArgsToString(Object[] args) {
    try {
      String raw = Arrays.toString(args);
      return raw.length() > MAX_ARGS_LENGTH ? raw.substring(0, MAX_ARGS_LENGTH) + "...]" : raw;
    } catch (Exception e) {
      return "[args-error: " + e.getClass().getSimpleName() + "]";
    }
  }

  private void logEntry(ContextView ctx, String methodId, String args) {
    log.info("{} ==> [{}] called | args: {}", getLogPrefix(ctx), methodId, args);
  }

  private void logSyncExit(String mId, Object res, long start, boolean logRes) {
    long duration = System.currentTimeMillis() - start;
    if (logRes) {
      String resStr = res != null ? String.valueOf(res) : "void";
      log.info(
          "{} <== [{}] completed in {}ms | result: {}", getLogPrefix(null), mId, duration, resStr);
    } else {
      log.info("{} <== [{}] completed in {}ms", getLogPrefix(null), mId, duration);
    }
  }

  private String getLogPrefix(ContextView ctx) {
    return String.format(
        "[traceId=%s, ip=%s, user=%s] [%s %s]",
        val(ctx, ZeabayConstants.TRACE_ID_CTX_KEY, "missing-trace-id"),
        val(ctx, ZeabayConstants.IP_CTX_KEY, "unknown-ip"),
        val(ctx, ZeabayConstants.USER_CTX_KEY, "-"),
        val(ctx, ZeabayConstants.METHOD_CTX_KEY, "-"),
        val(ctx, ZeabayConstants.PATH_CTX_KEY, "-"));
  }

  private String val(ContextView ctx, String key, String def) {
    if (ctx != null) {
      String ctxVal = ctx.getOrDefault(key, null);
      if (ctxVal != null) return ctxVal;
    }
    String mdc = MDC.get(key);
    return mdc != null ? mdc : def;
  }

  private <T> Mono<T> logMonoResult(
      Mono<T> mono, String mId, long start, boolean logRes, ContextView ctx) {
    return mono.doOnSuccess(
            v -> {
              long duration = System.currentTimeMillis() - start;
              if (logRes) {
                log.info(
                    "{} <== [{}] completed in {}ms | result: {}",
                    getLogPrefix(ctx),
                    mId,
                    duration,
                    v);
              } else {
                log.info("{} <== [{}] completed in {}ms", getLogPrefix(ctx), mId, duration);
              }
            })
        .doOnError(
            ex ->
                log.error(
                    "{} <== [{}] failed in {}ms. Reason: {}",
                    getLogPrefix(ctx),
                    mId,
                    (System.currentTimeMillis() - start),
                    ex.getMessage()))
        .doOnCancel(
            () ->
                log.warn(
                    "{} <== [{}] cancelled after {}ms",
                    getLogPrefix(ctx),
                    mId,
                    (System.currentTimeMillis() - start)));
  }

  private <T> Flux<T> logFluxResult(
      Flux<T> flux, String mId, long start, boolean logRes, ContextView ctx) {
    return flux.doOnComplete(
            () -> {
              long duration = System.currentTimeMillis() - start;
              log.info("{} <== [{}] stream completed in {}ms", getLogPrefix(ctx), mId, duration);
            })
        .doOnNext(
            v -> {
              if (logRes) {
                log.info("{}     [{}] emitted: {}", getLogPrefix(ctx), mId, v);
              }
            })
        .doOnError(
            ex ->
                log.error(
                    "{} <== [{}] flux failed in {}ms. Reason: {}",
                    getLogPrefix(ctx),
                    mId,
                    (System.currentTimeMillis() - start),
                    ex.getMessage()))
        .doOnCancel(
            () ->
                log.warn(
                    "{} <== [{}] flux cancelled after {}ms",
                    getLogPrefix(ctx),
                    mId,
                    (System.currentTimeMillis() - start)));
  }
}
