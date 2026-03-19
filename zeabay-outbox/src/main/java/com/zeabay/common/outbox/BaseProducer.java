package com.zeabay.common.outbox;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Base class for domain event producers. Automatically wraps events in the Outbox pattern within
 * the current DB transaction.
 *
 * <p>Each persisted {@link OutboxEvent} carries:
 *
 * <ul>
 *   <li>{@code eventId} — the originating domain event's TSID for distributed tracing
 *   <li>{@code producedFrom} — the producing service ({@code spring.application.name})
 *   <li>{@code producedAt} — set explicitly in Java (not left to DB DEFAULT)
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseProducer {

  protected final OutboxEventRepository outboxEventRepository;
  protected final ObjectMapper objectMapper;

  @Value("${spring.application.name}")
  private String producedFrom;

  /**
   * Serializes {@code payload} to JSON and persists a {@link OutboxEvent.Status#PENDING} outbox
   * record in the current transaction.
   *
   * @param eventId TSID of the originating domain event (for cross-service tracing)
   * @param eventType human-readable event type name
   * @param topic destination Kafka topic
   * @param aggregateType name of the aggregate root
   * @param aggregateId numeric TSID of the aggregate
   * @param payload event object to serialize
   * @param traceId propagated W3C trace identifier
   */
  protected Mono<Void> saveOutboxEvent(
      String eventId,
      String eventType,
      String topic,
      String aggregateType,
      Long aggregateId,
      Object payload,
      String traceId) {

    return serializePayload(payload, eventType)
        .map(
            json ->
                buildOutboxEvent(
                    eventId, eventType, topic, aggregateType, aggregateId, json, traceId))
        .flatMap(outboxEventRepository::save)
        .then();
  }

  private Mono<String> serializePayload(Object payload, String eventType) {
    try {
      return Mono.just(objectMapper.writeValueAsString(payload));
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize outbox event [eventType={}]: {}", eventType, e.getMessage());
      return Mono.error(new RuntimeException("Failed to serialize outbox event: " + eventType, e));
    }
  }

  private OutboxEvent buildOutboxEvent(
      String eventId,
      String eventType,
      String topic,
      String aggregateType,
      Long aggregateId,
      String jsonPayload,
      String traceId) {
    return OutboxEvent.builder()
        .eventId(eventId)
        .eventType(eventType)
        .topic(topic)
        .aggregateType(aggregateType)
        .aggregateId(aggregateId)
        .payload(jsonPayload)
        .producedFrom(producedFrom)
        .traceId(traceId)
        .status(OutboxEvent.Status.PENDING)
        .retryCount(0)
        .producedAt(Instant.now())
        .build();
  }
}
