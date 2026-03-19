package com.zeabay.common.outbox;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a persisted domain event. Must be saved in the same transaction as the domain
 * aggregate.
 */
@Getter
@Setter
@Builder
@Table("outbox_events")
public class OutboxEvent {

  @Id private Long id;

  @Column("event_id")
  private String eventId;

  @Column("event_type")
  private String eventType;

  @Column("topic")
  private String topic;

  @Column("aggregate_type")
  private String aggregateType;

  @Column("aggregate_id")
  private Long aggregateId;

  @Column("payload")
  private String payload;

  @Column("produced_from")
  private String producedFrom;

  @Column("trace_id")
  private String traceId;

  @Builder.Default
  @Column("status")
  private Status status = Status.PENDING;

  @Builder.Default
  @Column("retry_count")
  private int retryCount = 0;

  @Column("produced_at")
  private Instant producedAt;

  @Column("published_at")
  private Instant publishedAt;

  public enum Status {
    PENDING,
    PUBLISHED,
    FAILED
  }
}
