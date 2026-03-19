package com.zeabay.common.outbox;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * Reactive repository for OutboxEvent. Uses schema-qualified table names dynamically based on the
 * R2DBC connection.
 */
public interface OutboxEventRepository
    extends ReactiveCrudRepository<OutboxEvent, Long>, OutboxEventRepositoryCustom {}
