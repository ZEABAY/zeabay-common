package com.zeabay.common.api.model;

import lombok.Builder;

/** Represents a single field-level validation failure (e.g., Jakarta Validation). */
@Builder
public record ValidationError(String field, String message) {}
