package com.zeabay.common.autoconfigure;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;

import com.zeabay.common.constant.ZeabayConstants;
import com.zeabay.common.web.filter.ZeabayRequestContextWebFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

@AutoConfiguration
public class ZeabayTraceIdAutoConfiguration {

  private static final Pattern TRACEPARENT =
      Pattern.compile("^[\\da-fA-F]{2}-([\\da-fA-F]{32})-[\\da-fA-F]{16}-[\\da-fA-F]{2}$");

  private static final String HOOK_KEY = "zeabay-traceid-mdc-hook";

  private static String resolveTraceId(ServerWebExchange exchange) {
    String traceparent = exchange.getRequest().getHeaders().getFirst("traceparent");
    if (traceparent != null) {
      Matcher m = TRACEPARENT.matcher(traceparent.trim());
      if (m.matches()) {
        return m.group(1).toLowerCase();
      }
    }

    String xTraceId = exchange.getRequest().getHeaders().getFirst(ZeabayConstants.TRACE_ID_HEADER);
    if (xTraceId != null && !xTraceId.isBlank()) {
      return sanitize(xTraceId);
    }

    return UUID.randomUUID().toString().replace("-", "");
  }

  private static String sanitize(String raw) {
    String v = raw.trim();

    v = v.replace("\r", "").replace("\n", "");
    v = v.replaceAll("[^a-zA-Z0-9_-]", "");

    if (v.length() > 64) {
      v = v.substring(0, 64);
    }

    if (v.isBlank()) {
      return UUID.randomUUID().toString().replace("-", "");
    }
    return v;
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  @ConditionalOnWebApplication(type = REACTIVE)
  public WebFilter traceIdWebFilter() {
    return (exchange, chain) -> {
      String traceId = resolveTraceId(exchange);
      exchange.getResponse().getHeaders().set(ZeabayConstants.TRACE_ID_HEADER, traceId);
      exchange.getAttributes().put(ZeabayConstants.TRACE_ID_CTX_KEY, traceId);

      return chain
          .filter(exchange)
          .contextWrite(ctx -> ctx.put(ZeabayConstants.TRACE_ID_CTX_KEY, traceId));
    };
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE + 1)
  @ConditionalOnWebApplication(type = REACTIVE)
  public WebFilter zeabayRequestContextWebFilter() {
    return new ZeabayRequestContextWebFilter();
  }

  @Bean
  public Object reactorMdcTraceIdHook() {
    Hooks.resetOnEachOperator(HOOK_KEY);
    Hooks.onEachOperator(HOOK_KEY, Operators.lift((_, sub) -> new MdcLifter<>(sub)));
    return new Object();
  }

  @Bean
  @ConditionalOnClass(ExchangeFilterFunction.class)
  @ConditionalOnMissingBean(name = "zeabayTraceIdWebClientFilter")
  public ExchangeFilterFunction zeabayTraceIdWebClientFilter() {
    return (request, next) ->
        Mono.deferContextual(
            ctxView -> {
              String traceId = ctxView.getOrDefault(ZeabayConstants.TRACE_ID_CTX_KEY, null);
              if (traceId == null) return next.exchange(request);

              if (request.headers().getFirst(ZeabayConstants.TRACE_ID_HEADER) != null) {
                return next.exchange(request);
              }

              return next.exchange(
                  ClientRequest.from(request)
                      .header(ZeabayConstants.TRACE_ID_HEADER, traceId)
                      .build());
            });
  }

  private record MdcLifter<T>(CoreSubscriber<? super T> delegate) implements CoreSubscriber<T> {

    private static final String[] MDC_KEYS = {
      ZeabayConstants.TRACE_ID_CTX_KEY,
      ZeabayConstants.IP_CTX_KEY,
      ZeabayConstants.USER_CTX_KEY,
      ZeabayConstants.METHOD_CTX_KEY,
      ZeabayConstants.PATH_CTX_KEY
    };

    private static void withMdc(Context ctx, Runnable r) {
      Map<String, String> prevMdc = new HashMap<>();

      for (String key : MDC_KEYS) {
        Object val = ctx.getOrDefault(key, null);
        String prevVal = MDC.get(key);
        prevMdc.put(key, prevVal);

        if (val != null) {
          MDC.put(key, String.valueOf(val));
        } else {
          MDC.remove(key);
        }
      }

      try {
        r.run();
      } finally {
        for (String key : MDC_KEYS) {
          String prevVal = prevMdc.get(key);
          if (prevVal == null) {
            MDC.remove(key);
          } else {
            MDC.put(key, prevVal);
          }
        }
      }
    }

    @Override
    public Context currentContext() {
      return delegate.currentContext();
    }

    @Override
    public void onSubscribe(Subscription s) {
      delegate.onSubscribe(s);
    }

    @Override
    public void onNext(T t) {
      withMdc(delegate.currentContext(), () -> delegate.onNext(t));
    }

    @Override
    public void onError(Throwable t) {
      withMdc(delegate.currentContext(), () -> delegate.onError(t));
    }

    @Override
    public void onComplete() {
      withMdc(delegate.currentContext(), delegate::onComplete);
    }
  }
}
