package com.zeabay.common.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for zeabay-kafka auto-configuration.
 *
 * <p>Override in each service's {@code application.yml}:
 *
 * <pre>
 * zeabay:
 *   kafka:
 *     bootstrap-servers: localhost:9092
 *     producer:
 *       acks: all
 *       retries: 3
 *       retry-backoff-ms: 1000
 *     consumer:
 *       group-id-prefix: pulse
 *       auto-offset-reset: earliest
 *       max-poll-records: 100
 *     dlq:
 *       enabled: true
 *       suffix: ".dlq"
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "zeabay.kafka")
public class ZeabayKafkaProperties {

  /** Kafka bootstrap servers. Defaults to local dev. */
  private String bootstrapServers = "localhost:9092";

  private Producer producer = new Producer();
  private Consumer consumer = new Consumer();
  private Dlq dlq = new Dlq();

  @Data
  public static class Producer {
    /** Acks strategy: "all" means leader + all ISR replicas must ack (safest). */
    private String acks = "all";

    /** Number of retries on transient failures. */
    private int retries = 3;

    /** Milliseconds between retry attempts. */
    private long retryBackoffMs = 1000L;

    /** Idempotent producer — prevents duplicate messages on retry. */
    private boolean enableIdempotence = true;
  }

  @Data
  public static class Consumer {
    /**
     * Prefix for consumer group IDs. Each service appends its own suffix. e.g. "pulse" →
     * "pulse-mail-service".
     */
    private String groupIdPrefix = "pulse";

    /** What to do when no offset exists: "earliest" = replay from beginning. */
    private String autoOffsetReset = "earliest";

    /** Max records fetched per poll. */
    private int maxPollRecords = 100;

    /** Enable manual offset commit for exactly-once semantics. */
    private boolean enableAutoCommit = false;
  }

  @Data
  public static class Dlq {
    /** Whether to automatically route failed messages to a DLQ topic. */
    private boolean enabled = true;

    /** Suffix appended to the original topic name to form the DLQ topic. */
    private String suffix = ".dlq";

    /** Max delivery attempts before routing to DLQ. */
    private int maxAttempts = 3;
  }
}
