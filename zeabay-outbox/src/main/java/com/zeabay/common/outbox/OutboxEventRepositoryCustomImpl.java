package com.zeabay.common.outbox;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Native SQL implementation of {@link OutboxEventRepositoryCustom}.
 *
 * <p>Parses the R2DBC URL to derive the schema name, enabling schema-qualified table references for
 * multi-tenant deployments. Uses {@code FOR UPDATE SKIP LOCKED} to allow safe concurrent polling
 * across multiple service instances.
 */
@Slf4j
@Component
class OutboxEventRepositoryCustomImpl implements OutboxEventRepositoryCustom {

  private final DatabaseClient databaseClient;
  private final String schema;

  OutboxEventRepositoryCustomImpl(
      ConnectionFactory connectionFactory, @Value("${spring.r2dbc.url:}") String r2dbcUrl) {
    this.databaseClient = DatabaseClient.create(connectionFactory);
    this.schema = parseSchemaFromUrl(r2dbcUrl);
    log.info("OutboxEventRepository: using table {}", tableRef());
  }

  private static String parseSchemaFromUrl(String url) {
    if (url == null || url.isBlank()) return null;
    int q = url.indexOf('?');
    if (q < 0) return null;
    for (String param : url.substring(q + 1).split("&")) {
      int eq = param.indexOf('=');
      if (eq > 0 && "schema".equals(param.substring(0, eq).trim())) {
        String value = param.substring(eq + 1).trim();
        return value.isEmpty() ? null : value;
      }
    }
    return null;
  }

  private String tableRef() {
    return (schema != null) ? schema + ".outbox_events" : "outbox_events";
  }

  /**
   * {@inheritDoc}
   *
   * <p>Issues a single {@code UPDATE … RETURNING *} that increments {@code retry_count} and returns
   * the selected rows in one round trip, preventing phantom reads between SELECT and UPDATE.
   */
  @Override
  public Flux<OutboxEvent> findPendingEvents(int limit) {
    String sql =
        "UPDATE "
            + tableRef()
            + " SET retry_count = retry_count + 1 "
            + "WHERE id IN (SELECT id FROM "
            + tableRef()
            + " WHERE status = 'PENDING' ORDER BY produced_at ASC LIMIT $1 FOR UPDATE SKIP LOCKED) RETURNING *";
    return databaseClient.sql(sql).bind(0, limit).map(this::mapRow).all();
  }

  private OutboxEvent mapRow(Row row, RowMetadata metadata) {
    String statusStr = row.get("status", String.class);
    Integer retryCount = row.get("retry_count", Integer.class);
    return OutboxEvent.builder()
        .id(row.get("id", Long.class))
        .eventId(row.get("event_id", String.class))
        .eventType(row.get("event_type", String.class))
        .topic(row.get("topic", String.class))
        .aggregateType(row.get("aggregate_type", String.class))
        .aggregateId(row.get("aggregate_id", Long.class))
        .payload(row.get("payload", String.class))
        .traceId(row.get("trace_id", String.class))
        .status(
            statusStr != null ? OutboxEvent.Status.valueOf(statusStr) : OutboxEvent.Status.PENDING)
        .retryCount(retryCount != null ? retryCount : 0)
        .producedFrom(row.get("produced_from", String.class))
        .producedAt(row.get("produced_at", Instant.class))
        .publishedAt(row.get("published_at", Instant.class))
        .build();
  }
}
