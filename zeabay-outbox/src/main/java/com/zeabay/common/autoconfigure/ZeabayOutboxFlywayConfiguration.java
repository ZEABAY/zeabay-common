package com.zeabay.common.autoconfigure;

import io.r2dbc.spi.ConnectionFactory;
import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

/**
 * Runs Flyway migrations when {@code spring.flyway.enabled=true}.
 *
 * <p>Outbox table is created by Flyway migration (V0__outbox_events.sql), not by
 * ConnectionFactoryInitializer. All schema is under Flyway control.
 *
 * <p>Uses {@code spring.r2dbc.url}, {@code spring.r2dbc.username}, {@code spring.r2dbc.password}
 * directly (r2dbc: → jdbc: for URL). No duplicate config.
 */
@Configuration
@ConditionalOnBean(ConnectionFactory.class)
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
public class ZeabayOutboxFlywayConfiguration {

  private static final String OUTBOX_LOCATIONS =
      "classpath:db/migration,classpath:com/zeabay/common/outbox/db/migration";

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public ApplicationRunner flywayRunner(Environment env) {
    return args -> {
      String r2dbcUrl = env.getProperty("spring.r2dbc.url");
      if (r2dbcUrl == null || r2dbcUrl.isBlank()) {
        throw new IllegalStateException("Flyway requires spring.r2dbc.url");
      }
      String jdbcUrl = r2dbcUrl.replaceFirst("^r2dbc:", "jdbc:");
      String user = env.getProperty("spring.r2dbc.username", "");
      String password = env.getProperty("spring.r2dbc.password", "");

      String locations = env.getProperty("zeabay.flyway.locations", OUTBOX_LOCATIONS);

      Flyway flyway =
          Flyway.configure().dataSource(jdbcUrl, user, password).locations(locations).load();

      flyway.migrate();
    };
  }
}
