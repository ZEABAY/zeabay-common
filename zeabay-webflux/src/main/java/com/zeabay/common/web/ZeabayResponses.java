package com.zeabay.common.web;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import com.zeabay.common.api.exception.ErrorCode;
import com.zeabay.common.api.model.ErrorResponse;
import com.zeabay.common.api.model.ValidationError;
import com.zeabay.common.api.model.ZeabayApiResponse;
import com.zeabay.common.constant.ZeabayConstants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZeabayResponses {
  private static final String MISSING_VALUE = "missing";

  // -------- TraceId helpers --------

  public static String traceId(ContextView ctx) {
    Object v = ctx.getOrDefault(ZeabayConstants.TRACE_ID_CTX_KEY, MISSING_VALUE);
    return v != null ? v.toString() : MISSING_VALUE;
  }

  public static String traceId(ServerWebExchange exchange) {
    Object v = exchange.getAttribute(ZeabayConstants.TRACE_ID_CTX_KEY);
    if (v != null) return v.toString();

    // fallback
    return Optional.ofNullable(
            exchange.getRequest().getHeaders().getFirst(ZeabayConstants.TRACE_ID_HEADER))
        .orElse(MISSING_VALUE);
  }

  // -------- Success (service/controller) --------

  public static <T> Mono<ZeabayApiResponse<T>> ok(T data) {
    return Mono.deferContextual(ctx -> Mono.just(ZeabayApiResponse.ok(data, traceId(ctx))));
  }

  public static <T> Mono<ResponseEntity<ZeabayApiResponse<T>>> created(T data, URI location) {
    return Mono.deferContextual(
        ctx ->
            Mono.just(
                ResponseEntity.created(location).body(ZeabayApiResponse.ok(data, traceId(ctx)))));
  }

  // -------- Fail (typed, so it fits any endpoint return type) --------

  public static <T> Mono<ZeabayApiResponse<T>> fail(
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
                  code.name(), message, request.getPath().value(), now, validationErrors);
          return Mono.just(new ZeabayApiResponse<>(false, null, err, tid, now));
        });
  }

  // -------- Fail for ExceptionHandler (ResponseEntity) --------

  public static Mono<ResponseEntity<ZeabayApiResponse<Void>>> error(
      ServerWebExchange exchange,
      HttpStatus status,
      ErrorCode code,
      String message,
      List<ValidationError> validationErrors) {

    String tid = traceId(exchange);
    String path = exchange.getRequest().getPath().value();
    Instant now = Instant.now();

    ErrorResponse err = new ErrorResponse(code.name(), message, path, now, validationErrors);
    return Mono.just(ResponseEntity.status(status).body(ZeabayApiResponse.fail(err, tid)));
  }

  public static Mono<ResponseEntity<ZeabayApiResponse<Void>>> error(
      ServerWebExchange exchange, HttpStatus status, ErrorCode code, String message) {
    return error(exchange, status, code, message, List.of());
  }

  // -------- Fail for framework exceptions (no ErrorCode) --------

  public static Mono<ResponseEntity<ZeabayApiResponse<Void>>> error(
      ServerWebExchange exchange, HttpStatus status, String code, String message) {

    String tid = traceId(exchange);
    String path = exchange.getRequest().getPath().value();
    Instant now = Instant.now();

    ErrorResponse err = new ErrorResponse(code, message, path, now, List.of());
    return Mono.just(ResponseEntity.status(status).body(ZeabayApiResponse.fail(err, tid)));
  }
}
