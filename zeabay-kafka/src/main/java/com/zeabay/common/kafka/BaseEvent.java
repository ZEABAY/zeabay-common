package com.zeabay.common.kafka;

import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Standard envelope for all Kafka domain events in the Pulse platform.
 *
 * <p>Every event published to Kafka must extend this class. This ensures:
 *
 * <ul>
 *   <li>A consistent schema across all topics
 *   <li>Traceability via {@code traceId}
 *   <li>Deduplication support via TSID-based {@code eventId}
 *   <li>Event ordering/filtering via {@code occurredAt}
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>
 * public class EmailVerificationRequested extends BaseEvent {
 *   private final String userId;
 *   private final String email;
 *   private final String verificationToken;
 * }
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public abstract class BaseEvent {

  /** TSID-based unique event ID. Used for idempotency on consumer side. */
  private final String eventId;

  /** TraceId propagated from the originating request (W3C traceparent or X-Trace-Id). */
  private final String traceId;

  /** Wall-clock time when the event was created. */
  private final Instant occurredAt;

  /**
   * Logical event type name, e.g. "EmailVerificationRequested". Used by consumers to determine
   * deserialization target.
   */
  public abstract String getEventType();
}
