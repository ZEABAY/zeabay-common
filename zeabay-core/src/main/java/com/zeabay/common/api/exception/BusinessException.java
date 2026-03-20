package com.zeabay.common.api.exception;

import lombok.Getter;

/** Base exception for all domain and business rule violations across the platform. */
@Getter
public class BusinessException extends RuntimeException {
  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getDefaultMessage());
    this.errorCode = errorCode;
  }
}
