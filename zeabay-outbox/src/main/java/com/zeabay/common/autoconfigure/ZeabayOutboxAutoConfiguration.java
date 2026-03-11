package com.zeabay.common.autoconfigure;

import com.zeabay.common.outbox.OutboxEvent;
import com.zeabay.common.outbox.OutboxEventRepository;
import com.zeabay.common.outbox.OutboxProperties;
import com.zeabay.common.tsid.TsidGenerator;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.EnableScheduling;
import reactor.core.publisher.Mono;

/**
 * Autoconfigures the transactional outbox: repository, schema, and callbacks.
 *
 * <p>Activates when both {@link KafkaTemplate} and R2DBC are on the classpath. The actual
 * {@link com.zeabay.common.outbox.OutboxPublisher} is created by
 * {@link ZeabayOutboxPublisherAutoConfiguration} after this config runs.
 */
@AutoConfiguration
@EnableScheduling
@EnableR2dbcRepositories(basePackages = "com.zeabay.common.outbox")
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
   * <p>Uses the shared {@link TsidGenerator} bean so node ID configuration applies consistently.
   */
  @Bean
  @ConditionalOnMissingBean(name = "outboxTsidBeforeConvertCallback")
  public BeforeConvertCallback<OutboxEvent> outboxTsidBeforeConvertCallback(
      TsidGenerator tsidGenerator) {
    return (event, _) -> {
      if (event.getId() == null) {
        event.setId(tsidGenerator.newLongId());
      }
      return Mono.just(event);
    };
  }

}
