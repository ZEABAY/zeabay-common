package com.zeabay.common.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeabay.common.kafka.ZeabayKafkaProperties;
import com.zeabay.common.kafka.support.MapToPojoRecordMessageConverter;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

/**
 * Autoconfigures Kafka producer and consumer defaults for all Pulse services.
 *
 * <p>Services may override any bean by declaring their own {@code @Bean} of the same type. All
 * beans are {@code @ConditionalOnMissingBean}.
 */
@AutoConfiguration
@EnableKafka
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(ZeabayKafkaProperties.class)
public class ZeabayKafkaAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public ProducerFactory<String, Object> zeabayKafkaProducerFactory(ZeabayKafkaProperties props) {
    ZeabayKafkaProperties.Producer p = props.getProducer();
    Map<String, Object> config =
        Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers(),
            ProducerConfig.ACKS_CONFIG, p.getAcks(),
            ProducerConfig.RETRIES_CONFIG, p.getRetries(),
            ProducerConfig.RETRY_BACKOFF_MS_CONFIG, p.getRetryBackoffMs(),
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, p.isEnableIdempotence(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
    return new DefaultKafkaProducerFactory<>(config);
  }

  @Bean
  @ConditionalOnMissingBean
  public KafkaTemplate<String, Object> zeabayKafkaTemplate(
      ProducerFactory<String, Object> zeabayKafkaProducerFactory) {
    return new KafkaTemplate<>(zeabayKafkaProducerFactory);
  }

  @Bean
  @ConditionalOnMissingBean
  public ConsumerFactory<String, Object> zeabayKafkaConsumerFactory(ZeabayKafkaProperties props) {
    ZeabayKafkaProperties.Consumer c = props.getConsumer();
    Map<String, Object> config =
        Map.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            props.getBootstrapServers(),
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            c.getAutoOffsetReset(),
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG,
            c.getMaxPollRecords(),
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
            c.isEnableAutoCommit(),
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            JacksonJsonDeserializer.class,
            "spring.json.trusted.packages",
            c.getTrustedPackages());
    return new DefaultKafkaConsumerFactory<>(config);
  }

  @Bean
  @ConditionalOnMissingBean
  public ConcurrentKafkaListenerContainerFactory<String, Object>
      zeabayKafkaListenerContainerFactory(
          ConsumerFactory<String, Object> zeabayKafkaConsumerFactory,
          ObjectMapper objectMapper) {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(zeabayKafkaConsumerFactory);
    factory.setRecordMessageConverter(new MapToPojoRecordMessageConverter(objectMapper));
    return factory;
  }
}
