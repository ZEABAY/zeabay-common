package com.zeabay.common.outbox;

import reactor.core.publisher.Flux;

/**
 * Fragment interface for custom outbox queries (e.g. schema-qualified table names).
 */
interface OutboxEventRepositoryCustom {

  Flux<OutboxEvent> findPendingEvents(int limit);

  Flux<OutboxEvent> findRetryableEvents(int maxRetries, int limit);
}
