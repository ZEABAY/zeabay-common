package com.zeabay.common.web;

import com.zeabay.common.api.exception.BusinessException;
import com.zeabay.common.api.exception.ErrorCode;
import com.zeabay.common.api.model.ApiResponse;
import com.zeabay.common.api.model.ValidationError;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Order(-2)
@RestControllerAdvice
public class ZeabayGlobalExceptionHandler {

  @ExceptionHandler(WebExchangeBindException.class)
  public Mono<ResponseEntity<ApiResponse<Void>>> handleValidation(
      WebExchangeBindException ex, ServerWebExchange exchange) {

    List<ValidationError> errors =
        ex.getFieldErrors().stream().map(this::toValidationError).toList();
    return ZeabayResponses.error(
        exchange, HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Validation failed", errors);
  }

  @ExceptionHandler(BusinessException.class)
  public Mono<ResponseEntity<ApiResponse<Void>>> handleBusiness(
      BusinessException ex, ServerWebExchange exchange) {

    return ZeabayResponses.error(
        exchange, HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage());
  }

  @ExceptionHandler(ResponseStatusException.class)
  public Mono<ResponseEntity<ApiResponse<Void>>> handleResponseStatus(
      ResponseStatusException ex, ServerWebExchange exchange) {

    HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
    if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

    ErrorCode code =
        switch (status) {
          case NOT_FOUND -> ErrorCode.NOT_FOUND;
          case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
          case FORBIDDEN -> ErrorCode.FORBIDDEN;
          case BAD_REQUEST -> ErrorCode.BAD_REQUEST;
          default -> ErrorCode.INTERNAL_ERROR;
        };

    String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
    return ZeabayResponses.error(exchange, status, code, message);
  }

  @ExceptionHandler(Throwable.class)
  public Mono<ResponseEntity<ApiResponse<Void>>> handleAny(
      Throwable ex, ServerWebExchange exchange) {
    return ZeabayResponses.error(
        exchange, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Unexpected error");
  }

  private ValidationError toValidationError(FieldError fe) {
    return new ValidationError(
        fe.getField(), fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid");
  }
}
