package com.zeabay.common.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
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
 * Autoconfigures all Kafka infrastructure beans: admin client, producer factory, consumer factory,
 * error handler, listener container factory, and declarative topic creation.
 *
 * <p>All beans are guarded with {@link ConditionalOnMissingBean} so that applications can override
 * any individual bean without disabling the rest of the auto-configuration.
 */
@Slf4j
@AutoConfiguration
@EnableKafka
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(ZeabayKafkaProperties.class)
@AutoConfigureAfter(ZeabayCoreAutoConfiguration.class)
public class ZeabayKafkaAutoConfiguration {

  /**
   * Builds the full list of {@link NewTopic} definitions, including auto-generated DLQ topics when
   * {@link ZeabayKafkaProperties.Dlq#isEnabled()} is {@code true}.
   */
  @Bean
  public List<NewTopic> zeabayKafkaTopicDefinitions(ZeabayKafkaProperties props) {
    return buildAllTopics(props).stream()
        .map(
            t ->
                TopicBuilder.name(t.getName())
                    .partitions(t.getPartitions())
                    .replicas((int) t.getReplicationFactor())
                    .build())
        .toList();
  }

  /** Registers all topic definitions with {@link KafkaAdmin} for creation at startup. */
  @Bean
  public KafkaAdmin.NewTopics zeabayKafkaTopics(List<NewTopic> zeabayKafkaTopicDefinitions) {
    return zeabayKafkaTopicDefinitions.isEmpty()
        ? new KafkaAdmin.NewTopics()
        : new KafkaAdmin.NewTopics(zeabayKafkaTopicDefinitions.toArray(NewTopic[]::new));
  }

  /**
   * Creates the {@link KafkaAdmin} bean. {@code fatalIfBrokerNotAvailable} is {@code false} so that
   * the application starts even when Kafka is temporarily unavailable.
   */
  @Bean
  @ConditionalOnMissingBean
  public KafkaAdmin zeabayKafkaAdmin(ZeabayKafkaProperties props) {
    KafkaAdmin admin =
        new KafkaAdmin(
            Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers()));
    admin.setFatalIfBrokerNotAvailable(false);
    return admin;
  }

  /**
   * {@link ApplicationRunner} that verifies/creates topics at startup and logs their status (ready
   * vs. newly created). Broker errors are caught and logged as warnings — they do not fail startup.
   */
  @Bean
  public ApplicationRunner zeabayKafkaTopicLogger(
      ZeabayKafkaProperties props, List<NewTopic> zeabayKafkaTopicDefinitions) {
    return args -> {
      if (zeabayKafkaTopicDefinitions.isEmpty()) return;

      try (AdminClient admin =
          AdminClient.create(
              Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, props.getBootstrapServers()))) {

        Set<String> existing = admin.listTopics().names().get();
        List<NewTopic> missing =
            zeabayKafkaTopicDefinitions.stream()
                .filter(nt -> !existing.contains(nt.name()))
                .toList();

        Set<String> created = Set.of();
        if (!missing.isEmpty()) {
          admin.createTopics(missing).all().get();
          created = missing.stream().map(NewTopic::name).collect(Collectors.toSet());
        }

        Set<String> afterCreate = admin.listTopics().names().get();
        for (NewTopic nt : zeabayKafkaTopicDefinitions) {
          if (afterCreate.contains(nt.name())) {
            log.info(
                created.contains(nt.name()) ? "Kafka topic created: {}" : "Kafka topic ready: {}",
                nt.name());
          } else {
            log.warn("Kafka topic not found (may still be creating): {}", nt.name());
          }
        }
      } catch (Exception e) {
        log.warn("Could not create/verify Kafka topics: {}", e.getMessage());
      }
    };
  }

  private List<ZeabayKafkaProperties.Topic> buildAllTopics(ZeabayKafkaProperties props) {
    List<ZeabayKafkaProperties.Topic> all = new ArrayList<>(props.getTopics());
    ZeabayKafkaProperties.Dlq dlq = props.getDlq();

    if (dlq.isEnabled() && dlq.getSuffix() != null && !dlq.getSuffix().isEmpty()) {
      for (ZeabayKafkaProperties.Topic t : props.getTopics()) {
        String name = t.getName();
        if (name != null && !name.endsWith(dlq.getSuffix())) {
          String dlqName = name + dlq.getSuffix();
          if (all.stream().noneMatch(ot -> dlqName.equals(ot.getName()))) {
            ZeabayKafkaProperties.Topic dlqTopic = new ZeabayKafkaProperties.Topic();
            dlqTopic.setName(dlqName);
            dlqTopic.setPartitions(t.getPartitions());
            dlqTopic.setReplicationFactor(t.getReplicationFactor());
            all.add(dlqTopic);
          }
        }
      }
    }
    return all.stream().filter(t -> t.getName() != null).toList();
  }

  /** Creates the Kafka producer factory with idempotent, at-least-once delivery settings. */
  @Bean
  @ConditionalOnMissingBean
  public ProducerFactory<String, Object> zeabayKafkaProducerFactory(ZeabayKafkaProperties props) {
    ZeabayKafkaProperties.Producer p = props.getProducer();
    Map<String, Object> config =
        Map.of(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
            props.getBootstrapServers(),
            ProducerConfig.ACKS_CONFIG,
            p.getAcks(),
            ProducerConfig.RETRIES_CONFIG,
            p.getRetries(),
            ProducerConfig.RETRY_BACKOFF_MS_CONFIG,
            p.getRetryBackoffMs(),
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
            p.isEnableIdempotence(),
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(config);
  }

  /** Creates the shared {@link KafkaTemplate} with observation (tracing) enabled. */
  @Bean
  @ConditionalOnMissingBean
  public KafkaTemplate<String, Object> zeabayKafkaTemplate(
      ProducerFactory<String, Object> zeabayKafkaProducerFactory) {
    KafkaTemplate<String, Object> template = new KafkaTemplate<>(zeabayKafkaProducerFactory);
    template.setObservationEnabled(true);
    return template;
  }

  /** Creates the consumer factory using Jackson deserialization and trusted-packages config. */
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
            StringDeserializer.class);
    return new DefaultKafkaConsumerFactory<>(config);
  }

  /**
   * Creates the error handler. When DLQ is enabled, failed records are forwarded to the {@code
   * <topic>.dlq} partition-matched topic. Fixed backoff of 1 second is applied before each retry.
   */
  @Bean
  @ConditionalOnMissingBean
  public DefaultErrorHandler kafkaErrorHandler(
      KafkaTemplate<String, Object> template, ZeabayKafkaProperties props) {

    ZeabayKafkaProperties.Dlq dlqProps = props.getDlq();

    if (dlqProps.isEnabled()) {
      DeadLetterPublishingRecoverer recoverer =
          new DeadLetterPublishingRecoverer(
              template,
              (r, e) -> new TopicPartition(r.topic() + dlqProps.getSuffix(), r.partition()));
      return new DefaultErrorHandler(
          recoverer, new FixedBackOff(1000L, dlqProps.getMaxAttempts() - 1));
    }

    return new DefaultErrorHandler(new FixedBackOff(1000L, dlqProps.getMaxAttempts() - 1));
  }

  /**
   * Wires everything into the {@link ConcurrentKafkaListenerContainerFactory}: custom message
   * converter (Map→POJO), traceparent interceptor, error handler, and observation support.
   */
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
