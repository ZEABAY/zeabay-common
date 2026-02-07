package com.zeabay.common.autoconfigure;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;

import com.zeabay.common.tsid.TsidIdGenerator;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

@AutoConfiguration
public class ZeabayCommonAutoConfiguration {

  public static final String TRACE_ID_CTX_KEY = "traceId";
  public static final String TRACE_ID_HEADER = "X-Trace-Id";

  private static final Pattern TRACEPARENT =
      Pattern.compile("^[\\da-fA-F]{2}-([\\da-fA-F]{32})-[\\da-fA-F]{16}-[\\da-fA-F]{2}$");

  private static final String HOOK_KEY = "zeabay-traceid-mdc-hook";

  // ---------- TSID ----------

  private static String resolveTraceId(ServerWebExchange exchange) {
    String traceparent = exchange.getRequest().getHeaders().getFirst("traceparent");
    if (traceparent != null) {
      Matcher m = TRACEPARENT.matcher(traceparent.trim());
      if (m.matches()) {
        return m.group(1).toLowerCase();
      }
    }

    String xTraceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
    if (xTraceId != null && !xTraceId.isBlank()) {
      return sanitize(xTraceId);
    }

    return UUID.randomUUID().toString().replace("-", "");
  }

  // ---------- INBOUND (WebFlux) ----------

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

  // ---------- LOG (MDC) ----------

  @Bean
  @ConditionalOnMissingBean
  public TsidIdGenerator tsidIdGenerator() {
    return new TsidIdGenerator();
  }

  // ---------- OUTBOUND (WebClient) ----------

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  @ConditionalOnWebApplication(type = REACTIVE)
  public WebFilter traceIdWebFilter() {
    return (ServerWebExchange exchange, WebFilterChain chain) -> {
      String traceId = resolveTraceId(exchange);

      exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
      exchange.getAttributes().put(TRACE_ID_CTX_KEY, traceId);

      return chain.filter(exchange).contextWrite(ctx -> ctx.put(TRACE_ID_CTX_KEY, traceId));
    };
  }

  @Bean
  public Object reactorMdcTraceIdHook() {
    Hooks.resetOnEachOperator(HOOK_KEY);
    Hooks.onEachOperator(HOOK_KEY, Operators.lift((_, sub) -> new MdcLifter<>(sub)));
    return new Object();
  }

  // ---------- OpenAPI ----------

  @Bean
  @ConditionalOnClass(ExchangeFilterFunction.class)
  @ConditionalOnMissingBean(name = "zeabayTraceIdWebClientFilter")
  public ExchangeFilterFunction zeabayTraceIdWebClientFilter() {
    return (request, next) ->
        Mono.deferContextual(
            ctxView -> {
              String traceId = ctxView.getOrDefault(TRACE_ID_CTX_KEY, null);
              if (traceId == null) return next.exchange(request);

              if (request.headers().getFirst(TRACE_ID_HEADER) != null) {
                return next.exchange(request);
              }

              return next.exchange(
                  ClientRequest.from(request).header(TRACE_ID_HEADER, traceId).build());
            });
  }

  // ---------- Helpers ----------

  @Bean
  @ConditionalOnClass(WebClient.class)
  @ConditionalOnMissingBean(WebClient.class)
  public WebClient webClient(
      ObjectProvider<WebClient.Builder> builderProvider,
      ExchangeFilterFunction zeabayTraceIdWebClientFilter) {
    WebClient.Builder builder = builderProvider.getIfAvailable(WebClient::builder);

    return builder.filter(zeabayTraceIdWebClientFilter).build();
  }

  @Bean
  @ConditionalOnClass(OpenAPI.class)
  public OpenAPI zeabayOpenApi() {
    SecurityScheme bearer =
        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT");

    return new OpenAPI()
        .components(new Components().addSecuritySchemes("bearerAuth", bearer))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }

  private static final class MdcLifter<T> implements CoreSubscriber<T> {
    private final CoreSubscriber<? super T> delegate;

    private MdcLifter(CoreSubscriber<? super T> delegate) {
      this.delegate = delegate;
    }

    private static void withMdc(Context ctx, Runnable r) {
      Object traceIdObj = ctx.getOrDefault(TRACE_ID_CTX_KEY, null);

      // traceId yoksa MDC'ye dokunma
      if (traceIdObj == null) {
        r.run();
        return;
      }

      String newTraceId = String.valueOf(traceIdObj);
      String prev = MDC.get(TRACE_ID_CTX_KEY);

      MDC.put(TRACE_ID_CTX_KEY, newTraceId);
      try {
        r.run();
      } finally {
        // önceki değeri restore et
        if (prev == null) MDC.remove(TRACE_ID_CTX_KEY);
        else MDC.put(TRACE_ID_CTX_KEY, prev);
      }
    }

    @Override
    public Context currentContext() {
      return delegate.currentContext();
    }

    @Override
    public void onSubscribe(org.reactivestreams.Subscription s) {
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
