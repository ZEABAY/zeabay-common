package com.zeabay.common.web;

import com.zeabay.common.api.model.ApiResponse;
import com.zeabay.common.api.exception.BusinessException;
import com.zeabay.common.api.exception.ErrorCode;
import com.zeabay.common.api.model.ErrorResponse;
import com.zeabay.common.api.model.ValidationError;
import com.zeabay.common.autoconfigure.ZeabayCommonAutoConfiguration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Order(-2)
public class ZeabayGlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String traceId = traceId(exchange);
        String path = exchange.getRequest().getPath().value();

        List<ValidationError> errors = ex.getFieldErrors().stream()
                .map(this::toValidationError)
                .toList();

        ErrorResponse err = new ErrorResponse(
                ErrorCode.VALIDATION_ERROR.code(),
                "Validation failed",
                path,
                Instant.now(),
                errors
        );

        return Mono.just(ResponseEntity.badRequest().body(ApiResponse.fail(err, traceId)));
    }

    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleBusiness(BusinessException ex, ServerWebExchange exchange) {
        String traceId = traceId(exchange);
        String path = exchange.getRequest().getPath().value();

        ErrorResponse err = new ErrorResponse(
                ex.getErrorCode().code(),
                ex.getMessage(),
                path,
                Instant.now(),
                List.of()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(err, traceId)));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleResponseStatus(ResponseStatusException ex, ServerWebExchange exchange) {
        String traceId = traceId(exchange);
        String path = exchange.getRequest().getPath().value();

        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

        ErrorCode code = switch (status) {
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            case UNAUTHORIZED -> ErrorCode.UNAUTHORIZED;
            case FORBIDDEN -> ErrorCode.FORBIDDEN;
            case BAD_REQUEST -> ErrorCode.BAD_REQUEST;
            default -> ErrorCode.INTERNAL_ERROR;
        };

        ErrorResponse err = new ErrorResponse(
                code.code(),
                ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(),
                path,
                Instant.now(),
                List.of()
        );

        return Mono.just(ResponseEntity.status(status).body(ApiResponse.fail(err, traceId)));
    }

    @ExceptionHandler(Throwable.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleAny(Throwable ex, ServerWebExchange exchange) {
        String traceId = traceId(exchange);
        String path = exchange.getRequest().getPath().value();

        ErrorResponse err = new ErrorResponse(
                ErrorCode.INTERNAL_ERROR.code(),
                "Unexpected error",
                path,
                Instant.now(),
                List.of()
        );

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(err, traceId)));
    }

    // --- helpers

    private ValidationError toValidationError(FieldError fe) {
        return new ValidationError(fe.getField(), fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid");
    }

    private String traceId(ServerWebExchange exchange) {
        Object v = exchange.getAttribute(ZeabayCommonAutoConfiguration.TRACE_ID_CTX_KEY);
        if (v != null) return v.toString();

        // fallback
        String header = exchange.getRequest().getHeaders().getFirst(ZeabayCommonAutoConfiguration.TRACE_ID_HEADER);
        return header != null ? header : "missing";
    }
}
