package com.zeabay.common.autoconfigure;

import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.zeabay.common.kafka.ZeabayKafkaProperties;
import com.zeabay.common.kafka.support.MapToPojoRecordMessageConverter;
import com.zeabay.common.kafka.support.TraceparentRecordInterceptor;

import lombok.extern.slf4j.Slf4j;

/**
 * Autoconfigures core Kafka infrastructure: admin client, producer/consumer factories, error
 * handler, and listener container factory.
 *
 * <p>Note: Topic creation is delegated to domain-specific configuration classes (e.g.,
 * PulseKafkaTopicsConfiguration) providing {@link org.apache.kafka.clients.admin.NewTopic} beans.
 */
@Slf4j
@AutoConfiguration
@EnableKafka
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(ZeabayKafkaProperties.class)
@AutoConfigureAfter(ZeabayCoreAutoConfiguration.class)
public class ZeabayKafkaAutoConfiguration {

  private final String bootstrapServers;

  public ZeabayKafkaAutoConfiguration(Environment env) {
    this.bootstrapServers = env.getProperty("spring.kafka.bootstrap-servers", "localhost:9092");
  }

  /**
   * Creates the {@link KafkaAdmin} bean required for Spring to automatically create NewTopic beans
   * defined in shared modules.
   */
  @Bean
  @ConditionalOnMissingBean
  public KafkaAdmin zeabayKafkaAdmin() {
    KafkaAdmin admin =
        new KafkaAdmin(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
    admin.setFatalIfBrokerNotAvailable(false);
    return admin;
  }

  @Bean
  @ConditionalOnMissingBean
  public ProducerFactory<String, Object> zeabayKafkaProducerFactory(ZeabayKafkaProperties props) {
    ZeabayKafkaProperties.Producer p = props.getProducer();
    Map<String, Object> config =
        Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
            ProducerConfig.ACKS_CONFIG, p.getAcks(),
            ProducerConfig.RETRIES_CONFIG, p.getRetries(),
            ProducerConfig.RETRY_BACKOFF_MS_CONFIG, p.getRetryBackoffMs(),
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, p.isEnableIdempotence(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(config);
  }

  @Bean
  @ConditionalOnMissingBean
  public KafkaTemplate<String, Object> zeabayKafkaTemplate(
      ProducerFactory<String, Object> zeabayKafkaProducerFactory) {
    KafkaTemplate<String, Object> template = new KafkaTemplate<>(zeabayKafkaProducerFactory);
    template.setObservationEnabled(true);
    return template;
  }

  @Bean
  @ConditionalOnMissingBean
  public ConsumerFactory<String, Object> zeabayKafkaConsumerFactory(ZeabayKafkaProperties props) {
    ZeabayKafkaProperties.Consumer c = props.getConsumer();
    Map<String, Object> config =
        Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            c.getAutoOffsetReset(),
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
            c.getMaxPollRecords(),
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
            c.isEnableAutoCommit(),
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class);
    return new DefaultKafkaConsumerFactory<>(config);
  }

  /** Routes failed messages to a topic with the configured DLQ suffix. */
  @Bean
  @ConditionalOnMissingBean
  public DefaultErrorHandler kafkaErrorHandler(
      KafkaTemplate<String, Object> template, ZeabayKafkaProperties props) {

    ZeabayKafkaProperties.Dlq dlqProps = props.getDlq();

    if (dlqProps.isEnabled()) {
      DeadLetterPublishingRecoverer recoverer =
          new DeadLetterPublishingRecoverer(
              template,
              (r, _) -> new TopicPartition(r.topic() + dlqProps.getSuffix(), r.partition()));
      return new DefaultErrorHandler(
          recoverer, new FixedBackOff(1000L, dlqProps.getMaxAttempts() - 1));
    }

    return new DefaultErrorHandler(new FixedBackOff(1000L, dlqProps.getMaxAttempts() - 1));
  }

  @Bean
  @ConditionalOnMissingBean
  public ConcurrentKafkaListenerContainerFactory<String, Object>
      zeabayKafkaListenerContainerFactory(
          ConsumerFactory<String, Object> zeabayKafkaConsumerFactory,
          ObjectMapper objectMapper,
          DefaultErrorHandler kafkaErrorHandler) {

    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(zeabayKafkaConsumerFactory);
    factory.setRecordMessageConverter(new MapToPojoRecordMessageConverter(objectMapper));
    factory.setRecordInterceptor(new TraceparentRecordInterceptor<>());
    factory.getContainerProperties().setObservationEnabled(true);
    factory.setCommonErrorHandler(kafkaErrorHandler);

    return factory;
  }
}
