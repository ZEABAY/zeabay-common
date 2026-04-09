package com.zeabay.common.validation;

import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import com.zeabay.common.api.model.ValidationError;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Static utility for programmatic Bean Validation.
 *
 * <p>Use when validation needs to happen outside of controller binding (e.g., in domain services or
 * command handlers). The underlying {@link Validator} is set by {@link
 * com.zeabay.common.autoconfigure.ZeabayValidationAutoConfiguration} at startup.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZeabayValidator {

  private static Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

  /**
   * Replaces the default Hibernate-based validator with Spring's managed instance.
   *
   * @param springValidator the Spring-managed {@link Validator}
   */
  public static void setValidator(Validator springValidator) {
    VALIDATOR = springValidator;
    log.debug("ZeabayValidator is now backed by Spring's LocalValidatorFactoryBean");
  }

  /**
   * Validates the given object and returns a list of field-level errors.
   *
   * @param object the object to validate
   * @param <T> the type of the object
   * @return a list of {@link ValidationError}s, empty if the object is valid
   */
  public static <T> List<ValidationError> validate(T object) {
    Set<ConstraintViolation<T>> violations = VALIDATOR.validate(object);
    return violations.stream()
        .map(
            v -> {
              String code =
                  v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName();
              String messageKey = "validation." + v.getPropertyPath().toString() + "." + code;
              return new ValidationError(v.getPropertyPath().toString(), messageKey);
            })
        .toList();
  }

  /**
   * Returns {@code true} if the object passes all validation constraints.
   *
   * @param object the object to validate
   * @param <T> the type of the object
   * @return {@code true} if valid, {@code false} otherwise
   */
  public static <T> boolean isValid(T object) {
    return VALIDATOR.validate(object).isEmpty();
  }

  /**
   * Formats a list of validation errors into a comma-separated string for logging.
   *
   * @param errors the validation errors
   * @return a human-readable error summary
   */
  public static String formatErrors(List<ValidationError> errors) {
    return errors.stream()
        .map(e -> e.field() + " " + e.messageKey())
        .reduce((a, b) -> a + ", " + b)
        .orElse("");
  }
}
