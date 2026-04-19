package com.zeabay.common.kafka;

import java.time.Instant;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * The standard envelope for all Zeabay Kafka domain events.
 *
 * <p>Contract:
 *
 * <ul>
 *   <li>Ensures a consistent schema across all topics.
 *   <li>Supports Consumer Idempotency via TSID-based {@code eventId}.
 *   <li>Enforces chronological ordering/filtering via {@code occurredAt}.
 * </ul>
 *
 * <p>Uses {@link SuperBuilder} so child event classes inherit builder fields automatically. Child
 * classes should annotate themselves with {@code @SuperBuilder} and {@code @Jacksonized} — this
 * eliminates having to redeclare parent fields (eventId, traceId, occurredAt) in constructors.
 *
 * <p>JSON property naming and snake_case fallback are handled globally by the centralized {@link
 * com.fasterxml.jackson.databind.ObjectMapper} in {@code ZeabayCoreAutoConfiguration} — per-field
 * {@code @JsonProperty}/{@code @JsonAlias} annotations are not needed.
 */
@Getter
@SuperBuilder
public abstract class BaseEvent {

  private final String eventId;
  private final String traceId;
  private final Instant occurredAt;

  /**
   * Logical event name used by consumers for deserialization targeting (e.g. {@code
   * "EmailVerificationRequested"}).
   */
  public abstract String getEventType();
}
