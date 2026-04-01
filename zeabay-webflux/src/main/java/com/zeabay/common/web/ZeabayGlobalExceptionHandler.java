package com.zeabay.common.web;

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

import com.zeabay.common.api.exception.BusinessException;
import com.zeabay.common.api.exception.ErrorCode;
import com.zeabay.common.api.model.ValidationError;
import com.zeabay.common.api.model.ZeabayApiResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Order(-2)
@RestControllerAdvice
public class ZeabayGlobalExceptionHandler {

  @ExceptionHandler(WebExchangeBindException.class)
  public Mono<ResponseEntity<ZeabayApiResponse<Void>>> handleValidation(
      WebExchangeBindException ex, ServerWebExchange exchange) {

    List<ValidationError> errors =
        ex.getFieldErrors().stream().map(this::toValidationError).toList();
    return ZeabayResponses.error(
        exchange, HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "error.validation", errors);
  }

  @ExceptionHandler(BusinessException.class)
  public Mono<ResponseEntity<ZeabayApiResponse<Void>>> handleBusiness(
      BusinessException ex, ServerWebExchange exchange) {
    HttpStatus status = HttpStatus.valueOf(ex.getErrorCode().getHttpStatus());
    String messageKey = "error." + ex.getErrorCode().name().toLowerCase();
    return ZeabayResponses.error(exchange, status, ex.getErrorCode(), messageKey);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public Mono<ResponseEntity<ZeabayApiResponse<Void>>> handleResponseStatus(
      ResponseStatusException ex, ServerWebExchange exchange) {

    HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
    if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

    String messageKey = "error." + status.name().toLowerCase();
    return ZeabayResponses.error(exchange, status, status.name(), messageKey);
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<ZeabayApiResponse<Void>>> handleAny(
      Exception ex, ServerWebExchange exchange) {
    log.error("Unhandled exception processing request: {}", exchange.getRequest().getURI(), ex);
    return ZeabayResponses.error(
        exchange,
        HttpStatus.INTERNAL_SERVER_ERROR,
        ErrorCode.INTERNAL_ERROR,
        "error.internal_error");
  }

  private ValidationError toValidationError(FieldError fe) {
    String code = fe.getCode() != null ? fe.getCode() : "invalid";
    String messageKey = "validation." + fe.getField() + "." + code;
    return new ValidationError(fe.getField(), messageKey);
  }
}
