package com.zeabay.common.outbox;

import com.zeabay.common.autoconfigure.ZeabayR2dbcMigrationContributor;

/**
 * Registers the outbox module's Flyway migration path with the central {@link
 * com.zeabay.common.autoconfigure.ZeabayR2dbcFlywayConfiguration} runner.
 *
 * <p>Active only when Flyway is on the classpath. When Flyway is disabled, the {@code
 * outboxInitializer} bean in {@code ZeabayOutboxAutoConfiguration} handles table creation via R2DBC
 * instead.
 */
public class OutboxR2dbcMigrationContributor implements ZeabayR2dbcMigrationContributor {

  @Override
  public String getLocation() {
    return "classpath:com/zeabay/common/outbox/db/migration";
  }
}
