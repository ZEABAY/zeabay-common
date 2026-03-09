package com.zeabay.common.autoconfigure;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zeabay.common.security.OtpGenerator;
import com.zeabay.common.tsid.TsidIdGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ZeabayCommonAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public TsidIdGenerator tsidIdGenerator() {
    // TODO Dağıtık sistemlerde ID çakışmaması için önlem alınmalı
    // (TsidCreator.withNodeId(nodeId);)
    return new TsidIdGenerator();
  }

  /** Cryptographically random 6-digit OTP generator for email verification flows. */
  @Bean
  @ConditionalOnMissingBean
  public OtpGenerator otpGenerator() {
    return new OtpGenerator();
  }

  /**
   * ObjectMapper for REST API, Kafka, and outbox event serialization.
   *
   * <p>Frontend-friendly configuration:
   *
   * <ul>
   *   <li>camelCase: Matches JS/TS convention, no client transformation needed
   *   <li>ISO-8601 dates: Date.parse() compatible, no extra libs
   *   <li>Include nulls: Explicit contract for frontend, no absent vs null ambiguity
   *   <li>FAIL_ON_UNKNOWN_PROPERTIES=false: API versioning flexibility
   *   <li>FAIL_ON_EMPTY_BEANS=false: Empty objects serialize as {}
   * </ul>
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
