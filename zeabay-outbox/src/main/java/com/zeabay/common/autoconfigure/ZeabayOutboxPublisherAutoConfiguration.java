package com.zeabay.common.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import com.zeabay.common.outbox.OutboxEventRepository;
import com.zeabay.common.outbox.OutboxProperties;
import com.zeabay.common.outbox.OutboxPublisher;

import lombok.extern.slf4j.Slf4j;

/**
 * Initializes {@link OutboxPublisher} only after R2DBC and Kafka dependencies are fully
 * constructed.
 *
 * <p>Mirrors the inbox side: this configuration is intentionally separate from {@link
 * ZeabayOutboxAutoConfiguration} so that the publisher can be conditionally excluded in test
 * contexts.
 */
@Slf4j
@AutoConfiguration
@AutoConfigureAfter({ZeabayOutboxAutoConfiguration.class, ZeabayKafkaAutoConfiguration.class})
@ConditionalOnBean({OutboxEventRepository.class, KafkaTemplate.class})
public class ZeabayOutboxPublisherAutoConfiguration {

  /**
   * Creates the {@link OutboxPublisher} bean wired to the R2DBC repository, Kafka template,
   * configuration properties, and JSON mapper.
   */
  @Bean
  @SuppressWarnings("unchecked")
  public OutboxPublisher outboxPublisher(
      OutboxEventRepository repository,
      KafkaTemplate<String, ?> kafkaTemplate,
      OutboxProperties properties) {

    log.info("Initializing OutboxPublisher");
    return new OutboxPublisher(
        repository, (KafkaTemplate<String, Object>) kafkaTemplate, properties);
  }
}
