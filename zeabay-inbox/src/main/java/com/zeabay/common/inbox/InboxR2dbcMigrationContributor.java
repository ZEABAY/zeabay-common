package com.zeabay.common.inbox;

import com.zeabay.common.autoconfigure.ZeabayR2dbcMigrationContributor;

/**
 * Registers the inbox module's Flyway migration path with the central {@link
 * com.zeabay.common.autoconfigure.ZeabayR2dbcFlywayConfiguration} runner.
 *
 * <p>Active only when Flyway is on the classpath. When Flyway is disabled, the {@code
 * inboxInitializer} bean in {@code ZeabayInboxAutoConfiguration} handles table creation via R2DBC
 * instead.
 */
public class InboxR2dbcMigrationContributor implements ZeabayR2dbcMigrationContributor {

  @Override
  public String getLocation() {
    return "classpath:com/zeabay/common/inbox/db/migration";
  }
}
