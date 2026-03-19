package com.zeabay.common.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.zeabay.common.security.OtpGenerator;
import com.zeabay.common.tsid.TsidGenerator;

@AutoConfiguration
public class ZeabayCoreAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public TsidGenerator tsidGenerator() {
    return new TsidGenerator();
  }

  @Bean
  @ConditionalOnMissingBean
  public OtpGenerator otpGenerator() {
    return new OtpGenerator();
  }

  /**
   * Centralized ObjectMapper for REST API and Kafka serialization. Defines the platform's standard
   * JSON contract: lowerCamelCase, ISO-8601 strings.
   */
  @Bean
  @ConditionalOnClass(ObjectMapper.class)
  @ConditionalOnMissingBean(ObjectMapper.class)
  public ObjectMapper objectMapper() {
    return JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();
  }
}
