package com.zeabay.common.api.model;

import java.time.Instant;
import lombok.Builder;

@Builder
public record ZeabayApiResponse<T>(
    boolean success, T data, ErrorResponse error, String traceId, Instant timestamp) {
  public static <T> ZeabayApiResponse<T> ok(T data, String traceId) {
    return new ZeabayApiResponse<>(true, data, null, traceId, Instant.now());
  }

  public static <T> ZeabayApiResponse<T> fail(ErrorResponse error, String traceId) {
    return new ZeabayApiResponse<>(false, null, error, traceId, Instant.now());
  }
}
