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

/**
 * Factory methods for building standardized {@link ZeabayApiResponse} success and error responses.
 *
 * <p>Provides both service-level helpers (returning {@link Mono}) and exception-handler helpers
 * (returning {@link ResponseEntity}).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZeabayResponses {
  private static final String MISSING_VALUE = "missing";

  // -------- TraceId helpers --------

  /** Retrieves the trace ID from the Reactor context. */
  public static String traceId(ContextView ctx) {
    Object v = ctx.getOrDefault(ZeabayConstants.TRACE_ID_CTX_KEY, MISSING_VALUE);
    return v != null ? v.toString() : MISSING_VALUE;
  }

  /** Retrieves the trace ID from the server exchange attributes or request headers. */
  public static String traceId(ServerWebExchange exchange) {
    Object v = exchange.getAttribute(ZeabayConstants.TRACE_ID_CTX_KEY);
    if (v != null) return v.toString();

    // fallback
    return Optional.ofNullable(
            exchange.getRequest().getHeaders().getFirst(ZeabayConstants.TRACE_ID_HEADER))
        .orElse(MISSING_VALUE);
  }

  // -------- Success (service/controller) --------

  /** Creates a successful {@link ZeabayApiResponse} with the trace ID from Reactor context. */
  public static <T> Mono<ZeabayApiResponse<T>> ok(T data) {
    return Mono.deferContextual(ctx -> Mono.just(ZeabayApiResponse.ok(data, traceId(ctx))));
  }

  /** Creates a 201 CREATED response with the given data and location header. */
  public static <T> Mono<ResponseEntity<ZeabayApiResponse<T>>> created(T data, URI location) {
    return Mono.deferContextual(
        ctx ->
            Mono.just(
                ResponseEntity.created(location).body(ZeabayApiResponse.ok(data, traceId(ctx)))));
  }

  // -------- Fail (typed, so it fits any endpoint return type) --------

  /**
   * Creates a failed {@link ZeabayApiResponse} for use in service/controller methods.
   *
   * @param request the current HTTP request
   * @param code the application error code
   * @param messageKey the i18n message key
   * @param validationErrors optional field-level validation errors
   * @param <T> the response data type (typically unused)
   * @return a {@link Mono} emitting the error response
   */
  public static <T> Mono<ZeabayApiResponse<T>> fail(
      ServerHttpRequest request,
      ErrorCode code,
      String messageKey,
      List<ValidationError> validationErrors) {

    return Mono.deferContextual(
        ctx -> {
          String tid = traceId(ctx);
          Instant now = Instant.now();
          ErrorResponse err =
              new ErrorResponse(
                  code.name(), messageKey, request.getPath().value(), now, validationErrors);
          return Mono.just(new ZeabayApiResponse<>(false, null, err, tid, now));
        });
  }

  // -------- Fail for ExceptionHandler (ResponseEntity) --------

  /** Creates an error {@link ResponseEntity} for exception handlers with validation errors. */
  public static Mono<ResponseEntity<ZeabayApiResponse<Void>>> error(
      ServerWebExchange exchange,
      HttpStatus status,
      ErrorCode code,
      String messageKey,
      List<ValidationError> validationErrors) {

    String tid = traceId(exchange);
    String path = exchange.getRequest().getPath().value();
    Instant now = Instant.now();

    ErrorResponse err = new ErrorResponse(code.name(), messageKey, path, now, validationErrors);
    return Mono.just(ResponseEntity.status(status).body(ZeabayApiResponse.fail(err, tid)));
  }

  /** Convenience overload without validation errors. */
  public static Mono<ResponseEntity<ZeabayApiResponse<Void>>> error(
      ServerWebExchange exchange, HttpStatus status, ErrorCode code, String messageKey) {
    return error(exchange, status, code, messageKey, List.of());
  }

  // -------- Fail for framework exceptions (no ErrorCode) --------

  /**
   * Creates an error {@link ResponseEntity} for framework exceptions where no {@link ErrorCode} is
   * available.
   */
  public static Mono<ResponseEntity<ZeabayApiResponse<Void>>> error(
      ServerWebExchange exchange, HttpStatus status, String code, String messageKey) {

    String tid = traceId(exchange);
    String path = exchange.getRequest().getPath().value();
    Instant now = Instant.now();

    ErrorResponse err = new ErrorResponse(code, messageKey, path, now, List.of());
    return Mono.just(ResponseEntity.status(status).body(ZeabayApiResponse.fail(err, tid)));
  }
}
