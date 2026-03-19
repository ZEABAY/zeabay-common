package com.zeabay.common.kafka;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

import lombok.Data;

/**
 * Type-safe Kafka configuration properties. Configurable via {@code application.yml} under the
 * {@code zeabay.kafka} prefix.
 *
 * <p>Falls back to {@code spring.kafka.bootstrap-servers} if {@code bootstrapServers} is not set
 * explicitly.
 */
@Data
@ConfigurationProperties(prefix = "zeabay.kafka")
public class ZeabayKafkaProperties {

  @Autowired(required = false)
  private Environment environment;

  private String bootstrapServers;
  private List<Topic> topics = new ArrayList<>();
  private Producer producer = new Producer();
  private Consumer consumer = new Consumer();
  private Dlq dlq = new Dlq();

  /**
   * Returns the resolved bootstrap server address. Prefers the explicit {@code bootstrapServers}
   * property; falls back to {@code spring.kafka.bootstrap-servers}; defaults to {@code
   * localhost:9092}.
   */
  public String getBootstrapServers() {
    if (bootstrapServers != null && !bootstrapServers.isEmpty()) return bootstrapServers;
    return environment != null
        ? environment.getProperty("spring.kafka.bootstrap-servers", "localhost:9092")
        : "localhost:9092";
  }

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
    private String trustedPackages = "*";
  }

  /** Kafka topic definition used for declarative topic creation/validation at startup. */
  @Data
  public static class Topic {
    private String name;
    private int partitions = 1;
    private short replicationFactor = 1;
  }

  /** Dead-letter queue (DLQ) configuration. DLQ topics are named {@code <topic><suffix>}. */
  @Data
  public static class Dlq {
    private boolean enabled = true;
    private String suffix = ".dlq";
    private int maxAttempts = 5;
  }
}
