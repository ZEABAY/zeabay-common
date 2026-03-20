package com.zeabay.common.inbox;

import com.zeabay.common.kafka.BaseEvent;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import reactor.core.publisher.Mono;

/**
 * Base reactive consumer that enforces exactly-once processing via the Inbox pattern.
 *
 * <p><b>Idempotency strategy (Approach B):</b> Uses {@code repository.save()} and lets the
 * database's unique constraint on {@code (event_id, produced_from)} raise a {@link
 * DataIntegrityViolationException} for duplicates. Spring Data's {@code BeforeConvertCallback}
 * assigns the TSID {@code id} automatically.
 *
 * @param <T> the domain event type extending {@link BaseEvent}
 */
@Slf4j
public abstract class BaseConsumer<T extends BaseEvent> {

  @Autowired private InboxEventRepository inboxEventRepository;

  @Value("${spring.application.name}")
  private String producedFrom;

  /**
   * Call this from your {@code @KafkaListener} method. Uses {@code .block()} to ensure errors
   * propagate to the Kafka listener thread, enabling Spring Kafka's error handler (retry + DLQ).
   *
   * @param event the incoming domain event
   */
  public void handleEvent(T event) {
    processEvent(event).block();
  }

  /**
   * Saves an {@link InboxEvent} record (idempotency guard) then delegates to {@link #doProcess}.
   * Duplicate events (same {@code event_id} + {@code produced_from}) are silently discarded.
   *
   * @param event the incoming domain event
   * @return a {@link Mono} that completes when processing is done, or empty on duplicate
   */
  public Mono<Void> processEvent(T event) {
    InboxEvent record =
        InboxEvent.builder()
            .eventId(event.getEventId())
            .eventType(event.getEventType())
            .producedFrom(producedFrom)
            .traceId(event.getTraceId())
            .consumedAt(Instant.now())
            .build();

    return inboxEventRepository
        .save(record)
        .flatMap(
            _ ->
                doProcess(event)
                    .doOnSuccess(_ -> log.debug("Processed event: id={}", event.getEventId()))
                    .onErrorResume(
                        e -> {
                          log.error(
                              "Processing failed: id={}, error={}",
                              event.getEventId(),
                              e.getMessage());
                          return Mono.error(e);
                        }))
        .onErrorResume(
            DataIntegrityViolationException.class,
            _ -> {
              log.debug(
                  "Duplicate event skipped: id={}, producedFrom={}",
                  event.getEventId(),
                  producedFrom);
              return Mono.empty();
            });
  }

  /**
   * Implement event-specific processing logic.
   *
   * @param event the deduplicated domain event to process
   * @return a {@link Mono} that signals completion
   */
  protected abstract Mono<Void> doProcess(T event);
}
