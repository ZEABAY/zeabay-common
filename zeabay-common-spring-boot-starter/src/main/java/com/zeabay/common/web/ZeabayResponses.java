package com.zeabay.common.web;

import com.zeabay.common.api.exception.ErrorCode;
import com.zeabay.common.api.model.ApiResponse;
import com.zeabay.common.api.model.ErrorResponse;
import com.zeabay.common.api.model.ValidationError;
import com.zeabay.common.autoconfigure.ZeabayCommonAutoConfiguration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

public final class ZeabayResponses {

  private ZeabayResponses() {}

  // -------- TraceId helpers --------

  public static String traceId(ContextView ctx) {
    Object v = ctx.getOrDefault(ZeabayCommonAutoConfiguration.TRACE_ID_CTX_KEY, "missing");
    return v != null ? v.toString() : "missing";
  }

  public static String traceId(ServerWebExchange exchange) {
    Object v = exchange.getAttribute(ZeabayCommonAutoConfiguration.TRACE_ID_CTX_KEY);
    if (v != null) return v.toString();

    // fallback
    return Optional.ofNullable(
            exchange
                .getRequest()
                .getHeaders()
                .getFirst(ZeabayCommonAutoConfiguration.TRACE_ID_HEADER))
        .orElse("missing");
  }

  // -------- Success (service/controller) --------

  public static <T> Mono<ApiResponse<T>> ok(T data) {
    return Mono.deferContextual(ctx -> Mono.just(ApiResponse.ok(data, traceId(ctx))));
  }

  // -------- Fail (typed, so it fits any endpoint return type) --------

  public static <T> Mono<ApiResponse<T>> fail(
      ServerHttpRequest request,
      ErrorCode code,
      String message,
      List<ValidationError> validationErrors) {

    return Mono.deferContextual(
        ctx -> {
          String tid = traceId(ctx);
          Instant now = Instant.now();
          ErrorResponse err =
              new ErrorResponse(
                  code.code(), message, request.getPath().value(), now, validationErrors);
          return Mono.just(new ApiResponse<>(false, null, err, tid, now));
        });
  }

  // -------- Fail for ExceptionHandler (ResponseEntity) --------

  public static Mono<ResponseEntity<ApiResponse<Void>>> error(
      ServerWebExchange exchange,
      HttpStatus status,
      ErrorCode code,
      String message,
      List<ValidationError> validationErrors) {

    String tid = traceId(exchange);
    String path = exchange.getRequest().getPath().value();
    Instant now = Instant.now();

    ErrorResponse err = new ErrorResponse(code.code(), message, path, now, validationErrors);
    return Mono.just(ResponseEntity.status(status).body(ApiResponse.fail(err, tid)));
  }

  public static Mono<ResponseEntity<ApiResponse<Void>>> error(
      ServerWebExchange exchange, HttpStatus status, ErrorCode code, String message) {
    return error(exchange, status, code, message, List.of());
  }
}
