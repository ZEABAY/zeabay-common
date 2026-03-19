package com.zeabay.common.inbox;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a record of a processed domain event to ensure idempotency. Uses a surrogate Long
 * {@code id} as the DB primary key (assigned by {@code BeforeConvertCallback}), while {@code
 * (event_id, produced_from)} forms the dedup unique constraint.
 */
@Getter
@Setter
@Builder
@Table("inbox_events")
public class InboxEvent {

  @Id private Long id;

  @Column("event_id")
  private String eventId;

  @Column("event_type")
  private String eventType;

  @Column("produced_from")
  private String producedFrom;

  @Column("trace_id")
  private String traceId;

  @Column("consumed_at")
  private Instant consumedAt;
}
