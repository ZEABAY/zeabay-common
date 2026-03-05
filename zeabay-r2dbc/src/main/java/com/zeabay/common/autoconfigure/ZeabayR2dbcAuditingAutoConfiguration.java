package com.zeabay.common.autoconfigure;

import com.zeabay.common.r2dbc.BaseEntity;
import com.zeabay.common.tsid.TsidIdGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import reactor.core.publisher.Mono;

/**
 * Autoconfigures R2DBC auditing for all services using zeabay-r2dbc.
 *
 * <p>
 * Activates @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy on any
 * entity that
 * extends {@link BaseEntity}.
 *
 * <p>
 * Provides a fallback "system" auditor when no Spring Security context is
 * present. When {@code
 * zeabay-security} is on the classpath, it registers a security-aware
 * {@link ReactiveAuditorAware}
 * bean that overrides this default.
 */
@AutoConfiguration
@EnableR2dbcAuditing(auditorAwareRef = "zeabayReactiveAuditorAware")
public class ZeabayR2dbcAuditingAutoConfiguration {

  /**
   * Fallback auditor — returns "system" when no security context is available.
   */
  @Bean
  @ConditionalOnMissingBean(name = "zeabayReactiveAuditorAware")
  public ReactiveAuditorAware<String> zeabayReactiveAuditorAware() {
    return () -> Mono.just("system");
  }

  /**
   * Automatically assigns a TSID-based Long id to any {@link BaseEntity} before
   * it is converted to
   * a database row, if the id is not already set.
   */
  @Bean
  @ConditionalOnMissingBean(name = "zeabayTsidBeforeConvertCallback")
  public BeforeConvertCallback<BaseEntity> zeabayTsidBeforeConvertCallback() {
    TsidIdGenerator generator = new TsidIdGenerator();
    return (entity, table) -> {
      if (entity.getId() == null) {
        entity.setId(generator.newLongId());
      }
      return Mono.just(entity);
    };
  }
}
