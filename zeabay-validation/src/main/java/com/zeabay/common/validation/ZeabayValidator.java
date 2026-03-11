package com.zeabay.common.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility for programmatic Bean Validation.
 *
 * <p>Use this to validate objects outside a Spring MVC/WebFlux request context, e.g. inside a
 * service layer or before processing a Kafka message.
 *
 * <pre>
 *   var errors = ZeabayValidator.validate(myObject);
 *   if (!errors.isEmpty()) { ... }
 * </pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZeabayValidator {

  private static final ValidatorFactory FACTORY;
  private static final Validator VALIDATOR;

  static {
    // Factory and Validator must live for the application lifetime. Closing the factory
    // would destroy message interpolators, constraint caches, and cause validation failures.
    FACTORY = Validation.buildDefaultValidatorFactory();
    VALIDATOR = FACTORY.getValidator();
  }

  /**
   * Validates the given object and returns a list of {@link ValidationError}. Returns an empty list
   * if the object is valid.
   */
  public static <T> List<ValidationError> validate(T object) {
    Set<ConstraintViolation<T>> violations = VALIDATOR.validate(object);
    return violations.stream()
        .map(v -> new ValidationError(v.getPropertyPath().toString(), v.getMessage()))
        .toList();
  }

  /** Returns true if the given object passes all Bean Validation constraints. */
  public static <T> boolean isValid(T object) {
    return VALIDATOR.validate(object).isEmpty();
  }

  /**
   * Formats validation errors as a single comma-separated string (e.g. "email must be valid,
   * username must not be blank").
   */
  public static String formatErrors(List<ValidationError> errors) {
    return errors.stream()
        .map(e -> e.field() + " " + e.message())
        .reduce((a, b) -> a + ", " + b)
        .orElse("");
  }
}
