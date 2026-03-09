package com.zeabay.common.autoconfigure;

import com.zeabay.common.outbox.OutboxEvent;
import com.zeabay.common.outbox.OutboxEventRepository;
import com.zeabay.common.outbox.OutboxProperties;
import com.zeabay.common.outbox.OutboxPublisher;
import com.zeabay.common.tsid.TsidIdGenerator;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.publisher.Mono;

/**
 * Autoconfigures the transactional outbox publisher.
 *
 * <p>Activates only when both {@link KafkaTemplate} and R2DBC are on the classpath, so services
 * that don't use Kafka won't pick this up accidentally.
 */
@AutoConfiguration
@EnableScheduling
@ConditionalOnClass({KafkaTemplate.class, OutboxEventRepository.class})
@EnableConfigurationProperties(OutboxProperties.class)
public class ZeabayOutboxAutoConfiguration {

  /**
   * Creates the {@code outbox_events} table when Flyway is disabled.
   *
   * <p>When {@code spring.flyway.enabled=true}, Flyway migration V0__outbox_events.sql creates the
   * table instead. This initializer runs only when Flyway is off.
   */
  @Bean
  @ConditionalOnProperty(
      name = "spring.flyway.enabled",
      havingValue = "false",
      matchIfMissing = true)
  public ConnectionFactoryInitializer outboxInitializer(ConnectionFactory connectionFactory) {
    ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
    initializer.setConnectionFactory(connectionFactory);
    ResourceDatabasePopulator populator =
        new ResourceDatabasePopulator(
            new ClassPathResource("com/zeabay/common/outbox/db/migration/V0__outbox_events.sql"));
    initializer.setDatabasePopulator(populator);
    return initializer;
  }

  /**
   * Assigns a TSID to {@link OutboxEvent} before INSERT if id is null.
   *
   * <p>Intentionally duplicates the generic TSID callback pattern from {@code zeabay-r2dbc} to keep
   * {@code zeabay-outbox} independent — no cross-module dependency required.
   */
  @Bean
  @ConditionalOnMissingBean(name = "outboxTsidBeforeConvertCallback")
  public BeforeConvertCallback<OutboxEvent> outboxTsidBeforeConvertCallback() {
    TsidIdGenerator generator = new TsidIdGenerator();
    return (event, _) -> {
      if (event.getId() == null) {
        event.setId(generator.newLongId());
      }
      return Mono.just(event);
    };
  }

  @Bean
  @ConditionalOnBean({OutboxEventRepository.class, KafkaTemplate.class})
  public OutboxPublisher outboxPublisher(
      OutboxEventRepository repository,
      KafkaTemplate<String, Object> kafkaTemplate,
      OutboxProperties properties) {
    return new OutboxPublisher(repository, kafkaTemplate, properties);
  }
}
