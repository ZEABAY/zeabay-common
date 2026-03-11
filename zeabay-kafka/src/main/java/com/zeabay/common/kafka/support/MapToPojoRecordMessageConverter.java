package com.zeabay.common.kafka.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import org.springframework.kafka.support.converter.MessagingMessageConverter;
import org.springframework.lang.Nullable;

/**
 * Converts Kafka consumer records to Spring Messages, converting Map or JSON String payloads to
 * the target POJO type.
 *
 * <p>When the outbox publishes events, the payload is sent as a JSON object. JacksonJsonDeserializer
 * produces a {@link Map}. This converter converts that Map (or a JSON String from legacy messages)
 * to the listener's expected parameter type using Jackson.
 */
public class MapToPojoRecordMessageConverter extends MessagingMessageConverter {

  private final ObjectMapper objectMapper;

  public MapToPojoRecordMessageConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  protected Object extractAndConvertValue(
      org.apache.kafka.clients.consumer.ConsumerRecord<?, ?> record, @Nullable Type type) {
    Object value = record.value();
    if (value == null || type == null) {
      return value;
    }
    Class<?> targetClass = toClass(type);
    if (targetClass != null && needsConversion(value, targetClass)) {
      try {
        if (value instanceof Map<?, ?> map) {
          return objectMapper.convertValue(map, targetClass);
        }
        if (value instanceof String json) {
          return objectMapper.readValue(json, targetClass);
        }
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException("Failed to convert payload to " + type, e);
      }
    }
    return value;
  }

  @Nullable
  private static Class<?> toClass(Type type) {
    if (type instanceof Class<?> c) {
      return c;
    }
    if (type instanceof ParameterizedType pt) {
      Type raw = pt.getRawType();
      return raw instanceof Class<?> ? (Class<?>) raw : null;
    }
    return null;
  }

  private boolean needsConversion(Object value, Class<?> targetClass) {
    if (targetClass == Object.class) {
      return false;
    }
    if (targetClass.isInstance(value)) {
      return false;
    }
    return value instanceof Map || value instanceof String;
  }
}
