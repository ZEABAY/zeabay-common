package com.zeabay.common.api.model;

import java.time.Instant;
import lombok.Builder;

@Builder
public record ApiResponse<T>(
    boolean success, T data, ErrorResponse error, String traceId, Instant timestamp) {
  public static <T> ApiResponse<T> ok(T data, String traceId) {
    return new ApiResponse<>(true, data, null, traceId, Instant.now());
  }

  public static <T> ApiResponse<T> fail(ErrorResponse error, String traceId) {
    return new ApiResponse<>(false, null, error, traceId, Instant.now());
  }
}
