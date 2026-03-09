package com.zeabay.common.outbox;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Represents a single row in the {@code outbox_events} table.
 *
 * <p>The Transactional Outbox pattern works by writing this record in the <em>same database
 * transaction</em> as the domain operation. A background {@link OutboxPublisher} then polls for
 * unpublished events and sends them to Kafka.
 *
 * <p>Schema defined in {@code V0__outbox_events.sql} (single source for Flyway and
 * ConnectionFactoryInitializer).
 */
@Getter
@Setter
@Builder
@Table("outbox_events")
public class OutboxEvent {

  @Id private Long id;

  /** e.g. "EmailVerificationRequested" */
  @Column("event_type")
  private String eventType;

  /** Kafka topic to publish to. e.g. "pulse.auth.email-verification" */
  @Column("topic")
  private String topic;

  /** Domain aggregate type. e.g. "User" */
  @Column("aggregate_type")
  private String aggregateType;

  /** TSID of the aggregate instance. */
  @Column("aggregate_id")
  private Long aggregateId;

  /** JSON-serialized event payload. */
  @Column("payload")
  private String payload;

  /** TraceId from the originating request for distributed tracing. */
  @Column("trace_id")
  private String traceId;

  @Builder.Default
  @Column("status")
  private Status status = Status.PENDING;

  @Builder.Default
  @Column("retry_count")
  private int retryCount = 0;

  @Column("created_at")
  private Instant createdAt;

  @Column("published_at")
  private Instant publishedAt;

  public enum Status {
    PENDING,
    PUBLISHED,
    FAILED
  }
}
