package com.zeabay.common.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeabay.common.outbox.OutboxEventRepository;
import com.zeabay.common.outbox.OutboxProperties;
import com.zeabay.common.outbox.OutboxPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Creates {@link OutboxPublisher} after both {@link OutboxEventRepository} and {@link KafkaTemplate}
 * are available.
 *
 * <p>Split from {@link ZeabayOutboxAutoConfiguration} so that {@code @ConditionalOnBean} is
 * evaluated after the repository infrastructure has been created.
 */
@AutoConfiguration
@AutoConfigureAfter({ZeabayOutboxAutoConfiguration.class, ZeabayKafkaAutoConfiguration.class})
@ConditionalOnBean({OutboxEventRepository.class, KafkaTemplate.class})
public class ZeabayOutboxPublisherAutoConfiguration {

  @Bean
  @SuppressWarnings("unchecked")
  public OutboxPublisher outboxPublisher(
      OutboxEventRepository repository,
      KafkaTemplate<String, ?> kafkaTemplate,
      OutboxProperties properties,
      ObjectMapper objectMapper) {
    Logger log = LoggerFactory.getLogger(ZeabayOutboxPublisherAutoConfiguration.class);
    log.info("Creating OutboxPublisher bean (KafkaTemplate + OutboxEventRepository present)");
    return new OutboxPublisher(
        repository, (KafkaTemplate<String, Object>) kafkaTemplate, properties, objectMapper);
  }
}
