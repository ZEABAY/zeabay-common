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

  @Around("@annotation(loggable) || @within(loggable)")
  public Object logAround(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    if (loggable == null) {
      loggable = signature.getMethod().getDeclaringClass().getAnnotation(Loggable.class);
    }

    String methodId = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    boolean logArgs = loggable != null && loggable.logArgs();
    boolean logRes = loggable != null && loggable.logResult();
    String args =
        (logArgs && log.isDebugEnabled()) ? Arrays.toString(joinPoint.getArgs()) : HIDDEN_VALUE;

    Object result;
    long assemblyTime = System.currentTimeMillis();

    try {
      result = joinPoint.proceed();
    } catch (Throwable ex) {
      logEntry(null, methodId, args);
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
        logEntry(null, methodId, args);
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
        logEntry(null, methodId, args);
        logSyncExit(methodId, result, assemblyTime, logRes);
        yield result;
      }
    };
  }

  private void logEntry(ContextView ctx, String methodId, String args) {
    if (log.isDebugEnabled()) {
      log.debug("{} ==> [{}] called with args: {}", getLogPrefix(ctx), methodId, args);
    }
  }

  private void logSyncExit(String mId, Object res, long start, boolean logRes) {
    if (log.isDebugEnabled()) {
      long duration = System.currentTimeMillis() - start;
      String resStr =
          (logRes && res != null) ? String.valueOf(res) : (res == null ? "void" : HIDDEN_VALUE);
      log.debug(
          "{} <== [{}] returned in {}ms with result: {}",
          getLogPrefix(null),
          mId,
          duration,
          resStr);
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
    if (ctx != null) return ctx.getOrDefault(key, def);
    String mdc = MDC.get(key);
    return mdc != null ? mdc : def;
  }

  private <T> Mono<T> logMonoResult(
      Mono<T> mono, String mId, long start, boolean logRes, ContextView ctx) {
    return mono.doOnSuccess(
            v -> {
              if (log.isDebugEnabled()) {
                String res = logRes ? String.valueOf(v) : HIDDEN_VALUE;
                log.debug(
                    "{} <== [{}] completed in {}ms with result: {}",
                    getLogPrefix(ctx),
                    mId,
                    (System.currentTimeMillis() - start),
                    res);
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
              if (log.isDebugEnabled())
                log.debug(
                    "{} <== [{}] completed stream in {}ms",
                    getLogPrefix(ctx),
                    mId,
                    (System.currentTimeMillis() - start));
            })
        .doOnNext(
            v -> {
              if (logRes && log.isDebugEnabled())
                log.debug("{}     [{}] emitted: {}", getLogPrefix(ctx), mId, v);
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
