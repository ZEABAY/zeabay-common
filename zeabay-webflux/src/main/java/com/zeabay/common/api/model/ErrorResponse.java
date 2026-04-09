package com.zeabay.common.api.model;

import java.time.Instant;
import java.util.List;

import lombok.Builder;

/**
 * Standard error payload included in every failed {@link ZeabayApiResponse}.
 *
 * <p>Contains an error code, an i18n message key, the request path, timestamp, and optional
 * field-level validation errors.
 */
@Builder
public record ErrorResponse(
    String code,
    String messageKey,
    String path,
    Instant timestamp,
    List<ValidationError> validationErrors) {}
