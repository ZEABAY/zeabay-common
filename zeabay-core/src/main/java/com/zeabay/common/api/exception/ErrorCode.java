package com.zeabay.common.api.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Standardized application error codes mapped to HTTP status codes. */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  BAD_REQUEST("BAD_REQUEST", 400),
  VALIDATION_ERROR("VALIDATION_ERROR", 400),
  BUSINESS_ERROR("BUSINESS_ERROR", 400),
  UNAUTHORIZED("UNAUTHORIZED", 401),
  FORBIDDEN("FORBIDDEN", 403),
  NOT_FOUND("NOT_FOUND", 404),
  USER_ALREADY_EXISTS("USER_ALREADY_EXISTS", 409),
  INTERNAL_ERROR("INTERNAL_ERROR", 500);

  private final String code;
  private final int httpStatus;

  /** Resolves the closest ErrorCode fallback for unhandled framework/HTTP exceptions. */
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
