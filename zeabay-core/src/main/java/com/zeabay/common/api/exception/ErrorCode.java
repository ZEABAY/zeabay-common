package com.zeabay.common.api.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  VALIDATION_ERROR("VALIDATION_ERROR"),
  BUSINESS_ERROR("BUSINESS_ERROR"),
  NOT_FOUND("NOT_FOUND"),
  USER_ALREADY_EXISTS("USER_ALREADY_EXISTS"),
  UNAUTHORIZED("UNAUTHORIZED"),
  FORBIDDEN("FORBIDDEN"),
  BAD_REQUEST("BAD_REQUEST"),
  INTERNAL_ERROR("INTERNAL_ERROR");

  // TODO private final int httpStatus; eklenecek.
  private final String code;
}
