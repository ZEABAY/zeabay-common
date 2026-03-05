package com.zeabay.common.outbox;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * R2DBC repository for the {@code outbox_events} table.
 *
 * <p>Services use this to save outbox events alongside their domain entities in the <em>same</em>
 * R2DBC transaction. The {@link OutboxPublisher} polls this repository to dispatch pending events
 * to Kafka.
 */
public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEvent, Long> {

  /**
   * Returns the oldest PENDING events up to the given limit. Used by the publisher polling job for
   * batch dispatch.
   */
  @Query(
      """
      SELECT * FROM outbox_events
      WHERE status = 'PENDING'
      ORDER BY created_at ASC
      LIMIT :limit
      """)
  Flux<OutboxEvent> findPendingEvents(int limit);

  /** Returns FAILED events that haven't exceeded the retry limit. Used by the retry sweep job. */
  @Query(
      """
      SELECT * FROM outbox_events
      WHERE status = 'FAILED'
        AND retry_count < :maxRetries
      ORDER BY created_at ASC
      LIMIT :limit
      """)
  Flux<OutboxEvent> findRetryableEvents(int maxRetries, int limit);
}
