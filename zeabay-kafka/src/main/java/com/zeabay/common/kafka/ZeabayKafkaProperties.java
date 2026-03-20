package com.zeabay.common.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Type-safe Kafka configuration properties. Configurable via {@code application.yml} under the
 * {@code zeabay.kafka} prefix.
 *
 * <p>Bootstrap servers are resolved from Spring's standard {@code spring.kafka.bootstrap-servers}
 * property.
 */
@Data
@ConfigurationProperties(prefix = "zeabay.kafka")
public class ZeabayKafkaProperties {

  private Producer producer = new Producer();
  private Consumer consumer = new Consumer();
  private Dlq dlq = new Dlq();

  /** Kafka producer tuning parameters. */
  @Data
  public static class Producer {
    private String acks = "all";
    private int retries = 3;
    private long retryBackoffMs = 1000L;
    private boolean enableIdempotence = true;
  }

  /** Kafka consumer tuning parameters. */
  @Data
  public static class Consumer {
    private String autoOffsetReset = "earliest";
    private int maxPollRecords = 100;
    private boolean enableAutoCommit = false;
  }

  /** Dead-letter queue (DLQ) configuration for error handling logic. */
  @Data
  public static class Dlq {
    private boolean enabled = true;
    private String suffix = ".dlq";
    private int maxAttempts = 5;
  }
}
