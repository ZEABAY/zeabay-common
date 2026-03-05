package com.zeabay.common.outbox;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Polls the {@code outbox_events} table and publishes PENDING events to Kafka.
 *
 * <p><b>Flow:</b>
 *
 * <ol>
 *   <li>Every {@code zeabay.outbox.polling-interval-ms} ms, fetch up to {@code batchSize} PENDING
 *       events ordered by {@code created_at}.
 *   <li>Publish each event to its designated Kafka topic.
 *   <li>On success: mark status = PUBLISHED, set publishedAt.
 *   <li>On failure: increment retryCount; mark FAILED if retryCount >= maxRetries.
 * </ol>
 *
 * <p><b>Why polling instead of CDC (Debezium)?</b><br>
 * CDC requires additional infra (Kafka Connect, connector config, WAL access). Polling is simpler,
 * portable, and sufficient for current scale. We can switch to CDC later via an ADR without
 * changing the outbox schema.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

  private final OutboxEventRepository repository;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final OutboxProperties properties;

  @Scheduled(fixedDelayString = "${zeabay.outbox.polling-interval-ms:1000}")
  public void publishPendingEvents() {
    repository.findPendingEvents(properties.getBatchSize()).flatMap(this::publish).subscribe();
  }

  private Mono<OutboxEvent> publish(OutboxEvent event) {
    return Mono.fromCallable(
            () -> {
              kafkaTemplate.send(
                  event.getTopic(), String.valueOf(event.getAggregateId()), event.getPayload());
              return event;
            })
        .flatMap(
            e -> {
              e.setStatus(OutboxEvent.Status.PUBLISHED);
              e.setPublishedAt(Instant.now());
              log.debug(
                  "Outbox published: eventType={}, topic={}, aggregateId={}, traceId={}",
                  e.getEventType(),
                  e.getTopic(),
                  e.getAggregateId(),
                  e.getTraceId());
              return repository.save(e);
            })
        .onErrorResume(
            ex -> {
              log.warn(
                  "Outbox publish failed: eventType={}, retry={}, error={}",
                  event.getEventType(),
                  event.getRetryCount(),
                  ex.getMessage());
              event.setRetryCount(event.getRetryCount() + 1);
              if (event.getRetryCount() >= properties.getMaxRetries()) {
                event.setStatus(OutboxEvent.Status.FAILED);
                log.error(
                    "Outbox event permanently failed after {} retries: eventId={}",
                    properties.getMaxRetries(),
                    event.getId());
              }
              return repository.save(event);
            });
  }
}
