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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Runs Flyway migrations when {@code spring.flyway.enabled=true}.
 *
 * <p>Outbox table is created by Flyway migration (V0__outbox_events.sql), not by
 * ConnectionFactoryInitializer. All schema is under Flyway control.
 *
 * <p>Uses {@code spring.r2dbc.url}, {@code spring.r2dbc.username}, {@code spring.r2dbc.password}
 * directly (r2dbc: → jdbc: for URL). Schema from {@code schema} in R2DBC URL query string (e.g.
 * ?schema=auth).
 *
 * <p>Runs during context refresh (InitializingBean) so migrations complete before the app serves
 * requests.
 */
@AutoConfiguration
@AutoConfigureAfter(R2dbcAutoConfiguration.class)
@ConditionalOnBean(ConnectionFactory.class)
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
public class ZeabayOutboxFlywayConfiguration {

  private static final Logger log =
      LoggerFactory.getLogger(ZeabayOutboxFlywayConfiguration.class);

  private static final String OUTBOX_LOCATIONS =
      "classpath:db/migration,classpath:com/zeabay/common/outbox/db/migration";

  @Bean
  public ZeabayFlywayMigrationRunner zeabayFlywayMigrationRunner(Environment env) {
    return new ZeabayFlywayMigrationRunner(env);
  }

  static class ZeabayFlywayMigrationRunner implements InitializingBean {

    private final Environment env;

    ZeabayFlywayMigrationRunner(Environment env) {
      this.env = env;
    }

    @Override
    public void afterPropertiesSet() {
      log.info("ZeabayOutboxFlywayConfiguration: Running Flyway migrations");
      String r2dbcUrl = env.getProperty("spring.r2dbc.url");
      if (r2dbcUrl == null || r2dbcUrl.isBlank()) {
        throw new IllegalStateException("Flyway requires spring.r2dbc.url");
      }
      String jdbcUrl = r2dbcUrl.replaceFirst("^r2dbc:", "jdbc:");
      String user = env.getProperty("spring.r2dbc.username", "");
      String password = env.getProperty("spring.r2dbc.password", "");

      String schemaName = parseSchemaFromUrl(r2dbcUrl);
      if (schemaName == null || schemaName.isBlank()) {
        throw new IllegalStateException(
            "Flyway requires schema in spring.r2dbc.url (e.g. ?schema=auth)");
      }

      String locations = env.getProperty("zeabay.flyway.locations", OUTBOX_LOCATIONS);
      log.debug(
          "ZeabayOutboxFlywayConfiguration: schema={}, locations={}",
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
      log.info("ZeabayOutboxFlywayConfiguration: Flyway migrations completed");
    }

    private static String parseSchemaFromUrl(String url) {
      if (url == null || url.isBlank()) {
        return null;
      }
      int q = url.indexOf('?');
      if (q < 0) {
        return null;
      }
      String query = url.substring(q + 1);
      for (String param : query.split("&")) {
        int eq = param.indexOf('=');
        if (eq > 0 && "schema".equals(param.substring(0, eq).trim())) {
          String value = param.substring(eq + 1).trim();
          return value.isEmpty() ? null : value;
        }
      }
      return null;
    }
  }
}
