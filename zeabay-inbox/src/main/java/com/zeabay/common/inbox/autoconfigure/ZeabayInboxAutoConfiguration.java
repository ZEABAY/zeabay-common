package com.zeabay.common.inbox.autoconfigure;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

import com.zeabay.common.inbox.InboxEventRepository;
import com.zeabay.common.inbox.InboxProperties;
import com.zeabay.common.inbox.InboxR2dbcMigrationContributor;

import io.r2dbc.spi.ConnectionFactory;

/**
 * Autoconfigures Inbox pattern infrastructure: R2DBC repository and schema initializer. Mirrors
 * {@code ZeabayOutboxAutoConfiguration}.
 *
 * <p>Activates when both {@link KafkaTemplate} and {@link InboxEventRepository} are on the
 * classpath.
 *
 * <p>ID assignment for {@link com.zeabay.common.inbox.InboxEvent} is handled automatically by
 * {@code zeabayGenericTsidBeforeConvertCallback} in {@code ZeabayR2dbcAuditingAutoConfiguration} —
 * no dedicated {@code BeforeConvertCallback} bean is required here.
 */
@AutoConfiguration(after = R2dbcAutoConfiguration.class)
@EnableR2dbcRepositories(basePackages = "com.zeabay.common.inbox")
@ConditionalOnClass({KafkaTemplate.class, InboxEventRepository.class})
@EnableConfigurationProperties(InboxProperties.class)
public class ZeabayInboxAutoConfiguration {

  /**
   * Registers the inbox migration path with {@link
   * com.zeabay.common.autoconfigure.ZeabayR2dbcFlywayConfiguration} when Flyway is on the
   * classpath. No extra YAML location config needed.
   */
  @Bean
  @ConditionalOnClass(Flyway.class)
  public InboxR2dbcMigrationContributor inboxMigrationContributor() {
    return new InboxR2dbcMigrationContributor();
  }

  /**
   * Initializes the {@code inbox_events} table via R2DBC when Flyway is disabled. When Flyway is
   * enabled, the migration contributor above handles table creation instead.
   */
  @Bean
  @ConditionalOnProperty(
      name = "spring.flyway.enabled",
      havingValue = "false",
      matchIfMissing = true)
  public ConnectionFactoryInitializer inboxInitializer(ConnectionFactory connectionFactory) {
    ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
    initializer.setConnectionFactory(connectionFactory);
    initializer.setDatabasePopulator(
        new ResourceDatabasePopulator(
            new ClassPathResource("com/zeabay/common/inbox/db/migration/V0_1__inbox_events.sql")));
    return initializer;
  }
}
