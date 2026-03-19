package com.zeabay.common.inbox;

import org.springframework.data.r2dbc.repository.R2dbcRepository;

/**
 * Reactive repository for {@link InboxEvent}.
 *
 * <p>Duplicate detection relies on the database's UNIQUE constraint on {@code (event_id,
 * produced_from)}. A duplicate insert raises {@link
 * org.springframework.dao.DataIntegrityViolationException}, which {@link BaseConsumer} catches and
 * maps to {@link reactor.core.publisher.Mono#empty()}.
 *
 * <p>ID assignment is handled automatically by {@link
 * com.zeabay.common.autoconfigure.ZeabayR2dbcAuditingAutoConfiguration}'s {@code
 * zeabayGenericTsidBeforeConvertCallback} bean — no native query needed.
 */
public interface InboxEventRepository extends R2dbcRepository<InboxEvent, Long> {}
