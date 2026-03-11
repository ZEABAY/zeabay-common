package com.zeabay.common.api.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  // 4xx
  BAD_REQUEST("BAD_REQUEST", 400),
  VALIDATION_ERROR("VALIDATION_ERROR", 400),
  BUSINESS_ERROR("BUSINESS_ERROR", 400),
  UNAUTHORIZED("UNAUTHORIZED", 401),
  FORBIDDEN("FORBIDDEN", 403),
  NOT_FOUND("NOT_FOUND", 404),
  USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", 409),
  // 5xx
  INTERNAL_ERROR("INTERNAL_ERROR", 500);

  private final String code;
  private final int httpStatus;

  /**
   * Maps an HTTP status code to the closest ErrorCode. Used when handling ResponseStatusException
   * or other framework exceptions.
   */
  public static ErrorCode fromHttpStatus(int statusCode) {
    return switch (statusCode) {
      case 400 -> BAD_REQUEST;
      case 401 -> UNAUTHORIZED;
      case 403 -> FORBIDDEN;
      case 404 -> NOT_FOUND;
      case 409 -> USER_ALREADY_EXISTS;
      default -> INTERNAL_ERROR;
    };
  }
}
