package com.zeabay.common.outbox;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the outbox polling publisher.
 *
 * <pre>
 * zeabay:
 *   outbox:
 *     polling-interval: 1s    # How often to poll for PENDING events
 *     batch-size: 50          # How many events to process per poll
 *     max-retries: 3          # Max attempts before marking as FAILED permanently
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "zeabay.outbox")
public class OutboxProperties {

  /** How often to poll for PENDING outbox events. Default: 1 second. */
  private Duration pollingInterval = Duration.ofSeconds(1);

  /** Polling interval in milliseconds (alternative to polling-interval). */
  private long pollingIntervalMs = 1000;

  /** Max events to publish per polling cycle. */
  private int batchSize = 50;

  /** Max retry attempts before a FAILED event is no longer retried. */
  private int maxRetries = 3;
}
