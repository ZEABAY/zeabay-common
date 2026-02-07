package com.zeabay.common.api.model;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
    String code,
    String message,
    String path,
    Instant timestamp,
    List<ValidationError> validationErrors) {}
