package com.zeabay.common.outbox;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.time.Instant;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Custom repository implementation that supports schema-qualified table names.
 * Uses {@code spring.r2dbc.url} — extracts {@code schema} from query string (e.g.
 * ?schema=auth). Same schema as R2DBC connection.
 */
@Component
class OutboxEventRepositoryCustomImpl implements OutboxEventRepositoryCustom {

  private final DatabaseClient databaseClient;
  private final String schema;

  OutboxEventRepositoryCustomImpl(
      ConnectionFactory connectionFactory,
      @Value("${spring.r2dbc.url:}") String r2dbcUrl) {
    this.databaseClient = DatabaseClient.create(connectionFactory);
    this.schema = parseSchemaFromUrl(r2dbcUrl);
    LoggerFactory.getLogger(OutboxEventRepositoryCustomImpl.class)
        .info("OutboxEventRepository: using table {}", tableRef());
  }

  private static String parseSchemaFromUrl(String url) {
    if (url == null || url.isBlank()) {
      return null;
    }
    int q = url.indexOf('?');
    if (q < 0) {
      return null;
    }
    String query = url.substring(q + 1);
    for (String param : query.split("&")) {
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

  @Override
  public Flux<OutboxEvent> findPendingEvents(int limit) {
    String sql =
        "SELECT * FROM "
            + tableRef()
            + " WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT $1";
    return databaseClient.sql(sql).bind(0, limit).map(this::mapRow).all();
  }

  @Override
  public Flux<OutboxEvent> findRetryableEvents(int maxRetries, int limit) {
    String sql =
        "SELECT * FROM "
            + tableRef()
            + " WHERE status = 'FAILED' AND retry_count < $1 ORDER BY created_at ASC LIMIT $2";
    return databaseClient.sql(sql).bind(0, maxRetries).bind(1, limit).map(this::mapRow).all();
  }

  private OutboxEvent mapRow(Row row, RowMetadata metadata) {
    String statusStr = row.get("status", String.class);
    Integer retryCount = row.get("retry_count", Integer.class);
    return OutboxEvent.builder()
        .id(row.get("id", Long.class))
        .eventType(row.get("event_type", String.class))
        .topic(row.get("topic", String.class))
        .aggregateType(row.get("aggregate_type", String.class))
        .aggregateId(row.get("aggregate_id", Long.class))
        .payload(row.get("payload", String.class))
        .traceId(row.get("trace_id", String.class))
        .status(statusStr != null ? OutboxEvent.Status.valueOf(statusStr) : OutboxEvent.Status.PENDING)
        .retryCount(retryCount != null ? retryCount : 0)
        .createdAt(row.get("created_at", Instant.class))
        .publishedAt(row.get("published_at", Instant.class))
        .build();
  }
}
