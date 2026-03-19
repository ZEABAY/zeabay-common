package com.zeabay.common.autoconfigure;

import jakarta.validation.Validator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.zeabay.common.validation.ZeabayValidator;

@AutoConfiguration
public class ZeabayValidationAutoConfiguration {

  @Bean
  public InitializingBean zeabayValidatorInitializer(Validator springValidator) {
    return () -> ZeabayValidator.setValidator(springValidator);
  }
}
