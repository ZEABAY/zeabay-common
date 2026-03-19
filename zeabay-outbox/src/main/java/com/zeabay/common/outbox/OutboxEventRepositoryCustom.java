package com.zeabay.common.outbox;

import reactor.core.publisher.Flux;

/**
 * Custom repository operations for {@link OutboxEvent} that require native SQL.
 *
 * <p>Implemented by {@link OutboxEventRepositoryCustomImpl} and mixed into {@link
 * OutboxEventRepository} via Spring Data composition.
 */
public interface OutboxEventRepositoryCustom {

  /**
   * Atomically fetches and locks up to {@code limit} PENDING events for publishing.
   *
   * <p>Uses {@code FOR UPDATE SKIP LOCKED} to prevent concurrent processors from picking the same
   * rows, and increments {@code retry_count} in the same statement.
   *
   * @param limit maximum number of events to fetch
   * @return a {@link Flux} of locked, retry-incremented outbox events
   */
  Flux<OutboxEvent> findPendingEvents(int limit);
}
