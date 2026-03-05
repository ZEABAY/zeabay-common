package com.zeabay.common.validation;

/**
 * Represents a single field-level validation error.
 *
 * @param field the field path where the violation occurred (e.g. "email", "address.city")
 * @param message the human-readable violation message
 */
public record ValidationError(String field, String message) {}
