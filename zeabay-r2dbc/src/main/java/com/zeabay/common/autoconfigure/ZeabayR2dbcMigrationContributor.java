package com.zeabay.common.autoconfigure;

/**
 * SPI for registering additional Flyway migration locations with the central R2DBC Flyway runner.
 *
 * <p>Implement this interface and expose the implementation as a Spring bean to have your module's
 * migration scripts picked up automatically by {@link ZeabayR2dbcFlywayConfiguration}. The runner
 * always starts with {@code classpath:db/migration} (service-level migrations); every contributor
 * appends its own path.
 *
 * <p>Example:
 *
 * <pre>{@code
 * @Component
 * @ConditionalOnClass(Flyway.class)
 * class OutboxR2dbcMigrationContributor implements ZeabayR2dbcMigrationContributor {
 *     public String getLocation() {
 *         return "classpath:com/zeabay/common/outbox/db/migration";
 *     }
 * }
 * }</pre>
 */
public interface ZeabayR2dbcMigrationContributor {

  /**
   * Returns the Flyway-compatible classpath location of the migration scripts contributed by this
   * module (e.g., {@code "classpath:com/zeabay/common/outbox/db/migration"}).
   *
   * @return a non-null, non-blank classpath location string
   */
  String getLocation();
}
