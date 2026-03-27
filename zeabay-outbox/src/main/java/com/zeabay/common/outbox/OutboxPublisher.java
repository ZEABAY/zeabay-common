package com.zeabay.common.outbox;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import com.zeabay.common.logging.Loggable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Background worker that polls the database for {@link OutboxEvent #PENDING} outbox events and
 * publishes them to Kafka.
 *
 * <p>Uses optimistic locking ({@code FOR UPDATE SKIP LOCKED}) to safely run concurrently across
 * multiple service instances. Events that exceed {@link OutboxProperties#getMaxRetries()} are
 * marked {@link OutboxEvent.Status#FAILED}.
 */
@Slf4j
@Loggable
@RequiredArgsConstructor
public class OutboxPublisher {

  private static final java.security.SecureRandom SECURE_RANDOM = new SecureRandom();
  private final OutboxEventRepository repository;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final OutboxProperties properties;
  private final AtomicBoolean running = new AtomicBoolean(false);

  /**
   * Generates a W3C-compliant {@code traceparent} header value in the format {@code
   * 00-{traceId32}-{spanId16}-01} for distributed tracing correlation.
   *
   * @param traceId the raw trace identifier (may be null, blank, or a 32-char hex string)
   * @return a valid {@code traceparent} string
   */
  private static String formatTraceparent(String traceId) {
    String traceId32 = to32Hex(traceId);
    String spanId16 = HexFormat.of().formatHex(randomBytes(8));
    return "00-" + traceId32 + "-" + spanId16 + "-01";
  }

  /**
   * Normalises {@code traceId} to a 32-character lowercase hex string. Passes valid 32-char hex
   * strings through unchanged; hashes everything else with SHA-256; returns zeros for null/blank.
   *
   * @param traceId the raw trace identifier
   * @return a 32-character lowercase hex string
   */
  private static String to32Hex(String traceId) {
    if (traceId != null && traceId.matches("[a-fA-F0-9]{32}")) return traceId.toLowerCase();
    if (traceId == null || traceId.isBlank()) return "00000000000000000000000000000000";

    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(traceId.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash).substring(0, 32);
    } catch (NoSuchAlgorithmException e) {
      return "00000000000000000000000000000000";
    }
  }

  private static byte[] randomBytes(int length) {
    byte[] bytes = new byte[length];
    SECURE_RANDOM.nextBytes(bytes);
    return bytes;
  }

  @PostConstruct
  void logStartup() {
    log.info(
        "OutboxPublisher started: polling every {}ms, batchSize={}",
        properties.getPollingInterval().toMillis(),
        properties.getBatchSize());
  }

  /**
   * Scheduled polling loop. Fetches a batch of pending events and publishes each one to Kafka.
   * Re-entrant calls are skipped via {@link AtomicBoolean} guard.
   */
  @Scheduled(
      initialDelayString = "${zeabay.outbox.initial-delay-ms:5000}",
      fixedDelayString = "${zeabay.outbox.polling-interval-ms:1000}")
  public void publishPendingEvents() {
    if (!running.compareAndSet(false, true)) return;

    repository
        .findPendingEvents(properties.getBatchSize())
        .collectList()
        .filter(events -> !events.isEmpty())
        .doOnNext(events -> log.info("Outbox poll: processing {} pending event(s)", events.size()))
        .flatMapMany(Flux::fromIterable)
        .flatMap(this::publish)
        .doFinally(_ -> running.set(false))
        .subscribe(
            _ -> {},
            err -> log.error("Outbox poll cycle failed (Check DB schema/connection)", err));
  }

  /**
   * Deserializes the event payload, builds a Kafka {@link ProducerRecord} with a W3C traceparent
   * header, sends it, and marks the event {@link OutboxEvent.Status#PUBLISHED} on success or
   * increments retry / marks {@link OutboxEvent.Status#FAILED} on error.
   *
   * @param event the outbox event to publish
   * @return a {@link Mono} emitting the saved (updated) event
   */
  private Mono<OutboxEvent> publish(OutboxEvent event) {
    return Mono.fromCallable(
            () -> {
              String traceparent = formatTraceparent(event.getTraceId());
              ProducerRecord<String, Object> record =
                  new ProducerRecord<>(
                      event.getTopic(), String.valueOf(event.getAggregateId()), event.getPayload());
              record.headers().add("traceparent", traceparent.getBytes(StandardCharsets.UTF_8));
              return kafkaTemplate.send(record);
            })
        .flatMap(Mono::fromFuture)
        .thenReturn(event)
        .flatMap(
            e -> {
              e.setStatus(OutboxEvent.Status.PUBLISHED);
              e.setPublishedAt(Instant.now());
              log.debug(
                  "Outbox published: eventType={}, aggregateId={}",
                  e.getEventType(),
                  e.getAggregateId());
              return repository.save(e);
            })
        .onErrorResume(
            ex -> {
              log.warn(
                  "Outbox publish failed: eventType={}, retry={}, error={}",
                  event.getEventType(),
                  event.getRetryCount(),
                  ex.getMessage());
              if (event.getRetryCount() >= properties.getMaxRetries()) {
                event.setStatus(OutboxEvent.Status.FAILED);
                log.error("Outbox event permanently failed: eventId={}", event.getId());
              }
              return repository.save(event);
            });
  }
}
