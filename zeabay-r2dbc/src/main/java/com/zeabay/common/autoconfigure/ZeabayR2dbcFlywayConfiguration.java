package com.zeabay.common.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.zeabay.common.r2dbc.R2dbcUrlUtils;

import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * The single, central Flyway runner for all Zeabay R2DBC-backed services.
 *
 * <p>Activates when {@code spring.flyway.enabled=true} and a {@link ConnectionFactory} is present.
 * Always migrates {@code classpath:db/migration} (service-level migrations). Additional locations
 * are collected from every {@link ZeabayR2dbcMigrationContributor} bean on the classpath, so
 * modules like {@code zeabay-outbox} and {@code zeabay-inbox} register their paths automatically —
 * no extra YAML required.
 *
 * <p>Schema is derived from the {@code ?schema=} parameter in {@code spring.r2dbc.url}. Override
 * the full location list via {@code zeabay.flyway.locations} if needed.
 */
@AutoConfiguration
@AutoConfigureAfter(R2dbcAutoConfiguration.class)
@ConditionalOnBean(ConnectionFactory.class)
@ConditionalOnClass(Flyway.class)
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true")
public class ZeabayR2dbcFlywayConfiguration {

  /** Service-level migration scripts. Always included as the base location. */
  private static final String BASE_LOCATION = "classpath:db/migration";

  /**
   * Creates the migration runner bean, injecting the (potentially empty) list of module
   * contributors.
   *
   * @param env Spring environment for reading R2DBC / Flyway properties
   * @param contributors all {@link ZeabayR2dbcMigrationContributor} beans discovered on the
   *     classpath (empty list when no modules contribute)
   */
  @Bean
  public ZeabayR2dbcFlywayMigrationRunner zeabayR2dbcFlywayMigrationRunner(
      Environment env, List<ZeabayR2dbcMigrationContributor> contributors) {
    return new ZeabayR2dbcFlywayMigrationRunner(env, contributors);
  }

  @Slf4j
  static class ZeabayR2dbcFlywayMigrationRunner implements InitializingBean {

    private final Environment env;
    private final List<ZeabayR2dbcMigrationContributor> contributors;

    ZeabayR2dbcFlywayMigrationRunner(
        Environment env, List<ZeabayR2dbcMigrationContributor> contributors) {
      this.env = env;
      this.contributors = contributors;
    }

    private static String parseSchemaFromUrl(String url) {
      return R2dbcUrlUtils.parseSchema(url);
    }

    @Override
    public void afterPropertiesSet() {
      String r2dbcUrl = env.getProperty("spring.r2dbc.url");
      if (r2dbcUrl == null || r2dbcUrl.isBlank()) {
        throw new IllegalStateException(
            "ZeabayR2dbcFlywayConfiguration: spring.r2dbc.url is required");
      }

      String jdbcUrl = r2dbcUrl.replaceFirst("^r2dbc:", "jdbc:");
      String user = env.getProperty("spring.r2dbc.username", "");
      String password = env.getProperty("spring.r2dbc.password", "");
      String schemaName = parseSchemaFromUrl(r2dbcUrl);

      if (schemaName == null || schemaName.isBlank()) {
        throw new IllegalStateException(
            "ZeabayR2dbcFlywayConfiguration: schema required in spring.r2dbc.url (e.g. ?schema=auth)");
      }

      // Build location list: base + all module contributors (or override via property)
      String locationsProp = env.getProperty("zeabay.flyway.locations");
      String[] locations;
      if (locationsProp != null && !locationsProp.isBlank()) {
        locations = locationsProp.split(",");
      } else {
        List<String> locs = new ArrayList<>();
        locs.add(BASE_LOCATION);
        contributors.stream().map(ZeabayR2dbcMigrationContributor::getLocation).forEach(locs::add);
        locations = locs.toArray(String[]::new);
      }

      log.info(
          "ZeabayR2dbcFlywayConfiguration: running migrations for schema={}, locations={}",
          schemaName,
          String.join(", ", locations));

      Flyway.configure()
          .dataSource(jdbcUrl, user, password)
          .defaultSchema(schemaName)
          .schemas(schemaName)
          .createSchemas(true)
          .failOnMissingLocations(false)
          .locations(locations)
          .load()
          .migrate();

      log.info("ZeabayR2dbcFlywayConfiguration: migrations completed for schema={}", schemaName);
    }
  }
}
