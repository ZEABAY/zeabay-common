package com.zeabay.common.api.exception;

import lombok.Getter;

/** Base exception for all domain and business rule violations across the platform. */
@Getter
public class BusinessException extends RuntimeException {
  private final ErrorCode errorCode;

  /**
   * @param errorCode the standardized error code
   * @param message a human-readable detail message
   */
  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * Creates an exception using the error code's default message.
   *
   * @param errorCode the standardized error code
   */
  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getDefaultMessage());
    this.errorCode = errorCode;
  }
}
