package com.zeabay.common.logging;

import com.zeabay.common.constant.ZeabayConstants;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@Slf4j
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class LoggingAspect {

  public static final String TRACE_ID_CTX_KEY = "traceId";

  @Around("@annotation(loggable) || @within(loggable)")
  public Object logAround(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {

    if (loggable == null) {
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();
      loggable = signature.getMethod().getDeclaringClass().getAnnotation(Loggable.class);
    }

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    String className = signature.getDeclaringType().getSimpleName();
    String methodName = signature.getName();
    String methodId = className + "." + methodName;

    boolean logArgs = loggable != null && loggable.logArgs();
    boolean logResult = loggable != null && loggable.logResult();

    String argsString = logArgs ? Arrays.toString(joinPoint.getArgs()) : "[HIDDEN]";
    log.info("{} ==> [{}] called with args: {}", getLogPrefixFromMdc(), methodId, argsString);

    long startTime = System.currentTimeMillis();

    Object result;
    try {
      result = joinPoint.proceed();
    } catch (Throwable ex) {
      long duration = System.currentTimeMillis() - startTime;
      log.error(
          "{} <== [{}] threw exception after {}ms: {}",
          getLogPrefixFromMdc(),
          methodId,
          duration,
          ex.getMessage(),
          ex);
      throw ex;
    }

    if (result instanceof Mono<?> mono) {
      @SuppressWarnings("ReactiveStreamsUnusedPublisher")
      Object interceptedMono =
          mono.transformDeferredContextual(
              (originalMono, ctx) ->
                  logMonoResult(originalMono, methodId, startTime, logResult, ctx));
      return interceptedMono;
    } else if (result instanceof Flux<?> flux) {
      @SuppressWarnings("ReactiveStreamsUnusedPublisher")
      Object interceptedFlux =
          flux.transformDeferredContextual(
              (originalFlux, ctx) ->
                  logFluxResult(originalFlux, methodId, startTime, logResult, ctx));
      return interceptedFlux;
    } else {
      long duration = System.currentTimeMillis() - startTime;
      String resultString = logResult ? String.valueOf(result) : "[HIDDEN]";
      log.info(
          "{} <== [{}] returned in {}ms with result: {}",
          getLogPrefixFromMdc(),
          methodId,
          duration,
          resultString);
      return result;
    }
  }

  private String getLogPrefixFromMdc() {
    String traceId =
        MDC.get(TRACE_ID_CTX_KEY) != null ? MDC.get(TRACE_ID_CTX_KEY) : "missing-trace-id";
    String ip =
        MDC.get(ZeabayConstants.IP_CTX_KEY) != null
            ? MDC.get(ZeabayConstants.IP_CTX_KEY)
            : "unknown-ip";
    String user =
        MDC.get(ZeabayConstants.USER_CTX_KEY) != null ? MDC.get(ZeabayConstants.USER_CTX_KEY) : "-";
    String method =
        MDC.get(ZeabayConstants.METHOD_CTX_KEY) != null
            ? MDC.get(ZeabayConstants.METHOD_CTX_KEY)
            : "-";
    String path =
        MDC.get(ZeabayConstants.PATH_CTX_KEY) != null ? MDC.get(ZeabayConstants.PATH_CTX_KEY) : "-";

    return String.format("[traceId=%s, ip=%s, user=%s] [%s %s]", traceId, ip, user, method, path);
  }

  private String getLogPrefix(ContextView ctx) {
    String traceId = ctx.getOrDefault(TRACE_ID_CTX_KEY, "missing-trace-id");
    String ip = ctx.getOrDefault(ZeabayConstants.IP_CTX_KEY, "unknown-ip");
    String user = ctx.getOrDefault(ZeabayConstants.USER_CTX_KEY, "-");
    String method = ctx.getOrDefault(ZeabayConstants.METHOD_CTX_KEY, "-");
    String path = ctx.getOrDefault(ZeabayConstants.PATH_CTX_KEY, "-");

    return String.format("[traceId=%s, ip=%s, user=%s] [%s %s]", traceId, ip, user, method, path);
  }

  private <T> Mono<T> logMonoResult(
      Mono<T> mono, String methodId, long startTime, boolean logResult, ContextView ctx) {
    String logPrefix = getLogPrefix(ctx);
    return mono.doOnSuccess(
            value -> {
              long duration = System.currentTimeMillis() - startTime;
              String resultString = logResult ? String.valueOf(value) : "[HIDDEN]";
              log.info(
                  "{} <== [{}] completed in {}ms with result: {}",
                  logPrefix,
                  methodId,
                  duration,
                  resultString);
            })
        .doOnError(
            ex -> {
              long duration = System.currentTimeMillis() - startTime;
              log.error(
                  "{} <== [{}] failed in {}ms with error: {}",
                  logPrefix,
                  methodId,
                  duration,
                  ex.getMessage());
            })
        .doOnCancel(
            () -> {
              long duration = System.currentTimeMillis() - startTime;
              log.warn("{} <== [{}] cancelled after {}ms", logPrefix, methodId, duration);
            });
  }

  private <T> Flux<T> logFluxResult(
      Flux<T> flux, String methodId, long startTime, boolean logResult, ContextView ctx) {
    String logPrefix = getLogPrefix(ctx);
    return flux.doOnComplete(
            () -> {
              long duration = System.currentTimeMillis() - startTime;
              log.info("{} <== [{}] completed stream in {}ms", logPrefix, methodId, duration);
            })
        .doOnNext(
            value -> {
              if (logResult) {
                log.info("{}     [{}] emitted: {}", logPrefix, methodId, value);
              }
            })
        .doOnError(
            ex -> {
              long duration = System.currentTimeMillis() - startTime;
              log.error(
                  "{} <== [{}] flux failed in {}ms with error: {}",
                  logPrefix,
                  methodId,
                  duration,
                  ex.getMessage());
            })
        .doOnCancel(
            () -> {
              long duration = System.currentTimeMillis() - startTime;
              log.warn("{} <== [{}] flux cancelled after {}ms", logPrefix, methodId, duration);
            });
  }
}
