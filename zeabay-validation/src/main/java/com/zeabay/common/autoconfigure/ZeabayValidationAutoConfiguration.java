package com.zeabay.common.autoconfigure;

import jakarta.validation.Validator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.zeabay.common.validation.ZeabayValidator;

/**
 * Autoconfigures {@link ZeabayValidator} by injecting Spring's managed {@link Validator} instance.
 *
 * <p>This ensures Bean Validation uses the same validator factory as the rest of the application,
 * including any custom constraint validators managed by Spring.
 */
@AutoConfiguration
public class ZeabayValidationAutoConfiguration {

  /** Wires the Spring-managed validator into {@link ZeabayValidator} at startup. */
  @Bean
  public InitializingBean zeabayValidatorInitializer(Validator springValidator) {
    return () -> ZeabayValidator.setValidator(springValidator);
  }
}
