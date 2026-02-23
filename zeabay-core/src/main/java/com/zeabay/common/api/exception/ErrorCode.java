package com.zeabay.common.api.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  VALIDATION_ERROR("VALIDATION_ERROR"),
  BUSINESS_ERROR("BUSINESS_ERROR"),
  NOT_FOUND("NOT_FOUND"),
  UNAUTHORIZED("UNAUTHORIZED"),
  FORBIDDEN("FORBIDDEN"),
  BAD_REQUEST("BAD_REQUEST"),
  INTERNAL_ERROR("INTERNAL_ERROR");

  private final String code;
}
