package com.zeabay.common.kafka.support;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.KafkaNull;
import org.springframework.kafka.support.converter.MessagingMessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Converts Kafka consumer payloads (Map or JSON String) into target POJO types for
 * Spring @KafkaListener.
 */
@Slf4j
@RequiredArgsConstructor
public class MapToPojoRecordMessageConverter extends MessagingMessageConverter {

  private final ObjectMapper objectMapper;

  @Override
  protected Object extractAndConvertValue(ConsumerRecord<?, ?> record, Type type) {
    Object value = record.value();

    if (value == null) return KafkaNull.INSTANCE;
    if (type == null) return value;

    Class<?> targetClass = toClass(type);
    if (targetClass != null && needsConversion(value, targetClass)) {
      try {
        if (value instanceof Map<?, ?> map) return objectMapper.convertValue(map, targetClass);
        if (value instanceof String json) return objectMapper.readValue(json, targetClass);
      } catch (Exception e) {
        log.error("Failed to convert Kafka payload to target type: {}", type, e);
        throw new IllegalArgumentException("Failed to convert payload to " + type, e);
      }
    }
    return value;
  }

  private Class<?> toClass(Type type) {
    if (type instanceof Class<?> c) return c;
    if (type instanceof ParameterizedType pt) {
      Type raw = pt.getRawType();
      return raw instanceof Class<?> ? (Class<?>) raw : null;
    }
    return null;
  }

  private boolean needsConversion(Object value, Class<?> targetClass) {
    if (targetClass == Object.class || targetClass.isInstance(value)) return false;
    return value instanceof Map || value instanceof String;
  }
}
