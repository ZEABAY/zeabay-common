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

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZeabayValidator {

  private static Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

  public static void setValidator(Validator springValidator) {
    VALIDATOR = springValidator;
    log.debug("ZeabayValidator is now backed by Spring's LocalValidatorFactoryBean");
  }

  public static <T> List<ValidationError> validate(T object) {
    Set<ConstraintViolation<T>> violations = VALIDATOR.validate(object);
    return violations.stream()
        .map(v -> new ValidationError(v.getPropertyPath().toString(), v.getMessage()))
        .toList();
  }

  public static <T> boolean isValid(T object) {
    return VALIDATOR.validate(object).isEmpty();
  }

  public static String formatErrors(List<ValidationError> errors) {
    return errors.stream()
        .map(e -> e.field() + " " + e.message())
        .reduce((a, b) -> a + ", " + b)
        .orElse("");
  }
}
