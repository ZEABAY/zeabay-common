package com.zeabay.common.kafka;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
 */
@Getter
@RequiredArgsConstructor
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
