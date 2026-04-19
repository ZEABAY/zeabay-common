package com.zeabay.common.api.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Standardized application error codes mapped to HTTP status codes and default messages. */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // ── 400 Bad Request ──────────────────────────────────────────────────
  BAD_REQUEST(400, "Bad request"),
  VALIDATION_ERROR(400, "Validation failed"),
  BUSINESS_ERROR(400, "Business rule violation"),
  INVALID_INPUT(400, "Invalid input provided"),
  EMAIL_MISMATCH(400, "Email mismatch"),

  // ── 401 Unauthorized ─────────────────────────────────────────────────
  UNAUTHORIZED(401, "Authentication required"),
  TOKEN_EXPIRED(401, "Token has expired"),
  INVALID_TOKEN(401, "Invalid or malformed token"),
  INVALID_CREDENTIALS(401, "Invalid credentials"),
  TOKEN_ALREADY_USED(401, "Token has already been used"),

  // ── 403 Forbidden ────────────────────────────────────────────────────
  FORBIDDEN(403, "Access denied"),
  EMAIL_NOT_VERIFIED(403, "Email address not verified"),
  ACCOUNT_DISABLED(403, "Account has been disabled"),

  // ── 404 Not Found ────────────────────────────────────────────────────
  NOT_FOUND(404, "Resource not found"),
  USER_NOT_FOUND(404, "User not found"),
  PROFILE_NOT_FOUND(404, "Profile not found"),
  PROFILE_NOT_COMPLETED(404, "Profile not completed"),
  VERIFICATION_TOKEN_NOT_FOUND(404, "Verification token not found"),
  RESET_TOKEN_NOT_FOUND(404, "Password reset token not found"),

  // ── 409 Conflict ─────────────────────────────────────────────────────
  CONFLICT(409, "Resource conflict"),
  USER_ALREADY_EXISTS(409, "User already exists"),
  DUPLICATE_ENTRY(409, "Duplicate entry"),

  // ── 422 Unprocessable Entity ──────────────────────────────────────────
  AVATAR_INVALID_OWNERSHIP(422, "Avatar key does not belong to user"),
  AVATAR_UNSUPPORTED_TYPE(422, "Unsupported avatar file type"),
  AVATAR_FILE_TOO_LARGE(422, "Avatar file size exceeds maximum"),

  // ── 429 Too Many Requests ────────────────────────────────────────────
  RATE_LIMIT_EXCEEDED(429, "Too many requests"),

  // ── 500 Internal ─────────────────────────────────────────────────────
  INTERNAL_ERROR(500, "Internal server error"),

  // ── 503 Service Unavailable ──────────────────────────────────────────
  SERVICE_UNAVAILABLE(503, "Service temporarily unavailable"),

  // ── 502 Bad Gateway ──────────────────────────────────────────────────
  IDENTITY_PROVIDER_ERROR(502, "Identity provider error"),

  // ── 504 Gateway Timeout ──────────────────────────────────────────────
  GATEWAY_TIMEOUT(504, "Upstream service timed out");

  private final int httpStatus;
  private final String defaultMessage;
}
