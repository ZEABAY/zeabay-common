package com.zeabay.common.autoconfigure;

import io.r2dbc.spi.ConnectionFactory;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Runs Flyway migrations for R2DBC services when {@code spring.flyway.enabled=true}.
 *
 * <p>Used by services that have R2DBC + Flyway but no outbox (e.g. mail-service). Services with
 * zeabay-outbox use ZeabayOutboxFlywayConfiguration instead.
 *
 * <p>Does not load when {@code com.zeabay.common.outbox.OutboxEventRepository} is on classpath, so
 * outbox users are unaffected.
 */
@AutoConfiguration
@AutoConfigureAfter(R2dbcAutoConfiguration.class)
@ConditionalOnBean(ConnectionFactory.class)
@ConditionalOnClass(Flyway.class)
@ConditionalOnMissingClass("com.zeabay.common.outbox.OutboxEventRepository")
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
public class ZeabayR2dbcFlywayConfiguration {

  private static final Logger log =
      LoggerFactory.getLogger(ZeabayR2dbcFlywayConfiguration.class);

  @Bean
  public ZeabayR2dbcFlywayMigrationRunner zeabayR2dbcFlywayMigrationRunner(
      Environment env) {
    return new ZeabayR2dbcFlywayMigrationRunner(env);
  }

  static class ZeabayR2dbcFlywayMigrationRunner implements InitializingBean {

    private final Environment env;

    ZeabayR2dbcFlywayMigrationRunner(Environment env) {
      this.env = env;
    }

    @Override
    public void afterPropertiesSet() {
      log.info("ZeabayR2dbcFlywayConfiguration: Running Flyway migrations");
      String r2dbcUrl = env.getProperty("spring.r2dbc.url");
      if (r2dbcUrl == null || r2dbcUrl.isBlank()) {
        throw new IllegalStateException("Flyway requires spring.r2dbc.url");
      }
      String jdbcUrl = r2dbcUrl.replaceFirst("^r2dbc:", "jdbc:");
      String user = env.getProperty("spring.r2dbc.username", "");
      String password = env.getProperty("spring.r2dbc.password", "");

      String schema = env.getProperty("spring.flyway.schemas");
      if (schema == null || schema.isBlank()) {
        throw new IllegalStateException("Flyway requires spring.flyway.schemas");
      }

      String locations = env.getProperty("spring.flyway.locations");
      if (locations == null || locations.isBlank()) {
        throw new IllegalStateException("Flyway requires spring.flyway.locations");
      }

      String schemaName = schema.trim();
      log.debug(
          "ZeabayR2dbcFlywayConfiguration: schema={}, locations={}",
          schemaName,
          locations);
      Flyway.configure()
          .dataSource(jdbcUrl, user, password)
          .defaultSchema(schemaName)
          .schemas(schemaName)
          .createSchemas(true)
          .locations(locations.split(","))
          .load()
          .migrate();
      log.info("ZeabayR2dbcFlywayConfiguration: Flyway migrations completed");
    }
  }
}
