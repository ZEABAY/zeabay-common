package com.zeabay.common.outbox;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * R2DBC repository for the {@code outbox_events} table.
 *
 * <p>Services use this to save outbox events alongside their domain entities in the <em>same</em>
 * R2DBC transaction. The {@link OutboxPublisher} polls this repository to dispatch pending events
 * to Kafka.
 *
 * <p>Custom implementation ({@link OutboxEventRepositoryCustomImpl}) uses {@code schema} from
 * {@code spring.r2dbc.url} query string (e.g. ?schema=auth).
 */
public interface OutboxEventRepository
    extends ReactiveCrudRepository<OutboxEvent, Long>, OutboxEventRepositoryCustom {

  /**
   * Returns the oldest PENDING events up to the given limit. Implemented by {@link
   * OutboxEventRepositoryCustomImpl} to support schema-qualified table names.
   */
  Flux<OutboxEvent> findPendingEvents(int limit);

  /**
   * Returns FAILED events that haven't exceeded the retry limit. Implemented by {@link
   * OutboxEventRepositoryCustomImpl} to support schema-qualified table names.
   */
  Flux<OutboxEvent> findRetryableEvents(int maxRetries, int limit);
}
