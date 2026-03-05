package com.zeabay.common.autoconfigure;

import com.zeabay.common.outbox.OutboxEventRepository;
import com.zeabay.common.outbox.OutboxProperties;
import com.zeabay.common.outbox.OutboxPublisher;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.scheduling.annotation.EnableScheduling;

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

  @Bean
  public ConnectionFactoryInitializer outboxInitializer(ConnectionFactory connectionFactory) {
    ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
    initializer.setConnectionFactory(connectionFactory);
    ResourceDatabasePopulator populator =
        new ResourceDatabasePopulator(new ClassPathResource("schema-outbox.sql"));
    initializer.setDatabasePopulator(populator);
    return initializer;
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
