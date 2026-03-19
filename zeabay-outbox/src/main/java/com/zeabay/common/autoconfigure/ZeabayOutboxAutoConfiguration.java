package com.zeabay.common.autoconfigure;

import org.flywaydb.core.Flyway;
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

import com.zeabay.common.outbox.OutboxEvent;
import com.zeabay.common.outbox.OutboxEventRepository;
import com.zeabay.common.outbox.OutboxProperties;
import com.zeabay.common.outbox.OutboxR2dbcMigrationContributor;
import com.zeabay.common.tsid.TsidGenerator;

import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

/**
 * Autoconfigures Transactional Outbox infrastructure (R2DBC repository, schema, and TSID
 * generation).
 */
@AutoConfiguration
@EnableScheduling
@EnableR2dbcRepositories(basePackages = "com.zeabay.common.outbox")
@ConditionalOnClass({KafkaTemplate.class, OutboxEventRepository.class})
@EnableConfigurationProperties(OutboxProperties.class)
public class ZeabayOutboxAutoConfiguration {

  /**
   * Registers the outbox migration path with {@link
   * com.zeabay.common.autoconfigure.ZeabayR2dbcFlywayConfiguration} when Flyway is on the
   * classpath. This is the preferred way — no extra YAML needed.
   */
  @Bean
  @ConditionalOnClass(Flyway.class)
  public OutboxR2dbcMigrationContributor outboxMigrationContributor() {
    return new OutboxR2dbcMigrationContributor();
  }

  /** Creates outbox_events table only if Flyway is disabled. */
  @Bean
  @ConditionalOnProperty(
      name = "spring.flyway.enabled",
      havingValue = "false",
      matchIfMissing = true)
  public ConnectionFactoryInitializer outboxInitializer(ConnectionFactory connectionFactory) {
    ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
    initializer.setConnectionFactory(connectionFactory);
    initializer.setDatabasePopulator(
        new ResourceDatabasePopulator(
            new ClassPathResource("com/zeabay/common/outbox/db/migration/V0__outbox_events.sql")));
    return initializer;
  }

  /** Automatically assigns a collision-resistant TSID before database insertion. */
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
