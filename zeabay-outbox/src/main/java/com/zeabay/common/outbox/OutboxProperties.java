package com.zeabay.common.outbox;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/** Configuration properties for the Outbox polling mechanism (prefix: {@code zeabay.outbox}). */
@Data
@ConfigurationProperties(prefix = "zeabay.outbox")
public class OutboxProperties {
  private Duration pollingInterval = Duration.ofSeconds(1);
  private int batchSize = 50;
  private int maxRetries = 3;
}
