package com.zeabay.common.autoconfigure;

import com.zeabay.common.r2dbc.BaseEntity;
import com.zeabay.common.tsid.TsidIdGenerator;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import reactor.core.publisher.Mono;

/**
 * Autoconfigures R2DBC auditing for all services using zeabay-r2dbc.
 *
 * <p>Activates @CreatedDate, @LastModifiedDate, @CreatedBy, @LastModifiedBy on any entity that
 * extends {@link BaseEntity}.
 *
 * <p>Provides a fallback "system" auditor when no Spring Security context is present. When {@code
 * zeabay-security} is on the classpath, it registers a security-aware {@link ReactiveAuditorAware}
 * bean that overrides this default.
 */
@Slf4j
@AutoConfiguration
@EnableR2dbcAuditing(auditorAwareRef = "zeabayReactiveAuditorAware")
public class ZeabayR2dbcAuditingAutoConfiguration {

  /** Fallback auditor — returns "system" when no security context is available. */
  @Bean
  @ConditionalOnMissingBean(name = "zeabayReactiveAuditorAware")
  public ReactiveAuditorAware<String> zeabayReactiveAuditorAware() {
    return () -> Mono.just("system");
  }

  /** Assigns a TSID to any {@link BaseEntity} before INSERT if id is null. */
  @Bean
  @ConditionalOnMissingBean(name = "zeabayTsidBeforeConvertCallback")
  public BeforeConvertCallback<BaseEntity> zeabayTsidBeforeConvertCallback(
      TsidIdGenerator tsidIdGenerator) {
    return (entity, table) -> {
      if (entity.getId() == null) {
        entity.setId(tsidIdGenerator.newLongId());
      }
      return Mono.just(entity);
    };
  }

  /**
   * Assigns a TSID to any non-{@link BaseEntity} that declares an {@code @Id}-annotated {@code
   * Long} field with a null value before INSERT.
   *
   * <p>This covers entities like {@code AuthVerificationToken} that intentionally do not extend
   * {@link BaseEntity} (no audit columns needed) but still require a TSID primary key.
   */
  @Bean
  @ConditionalOnMissingBean(name = "zeabayGenericTsidBeforeConvertCallback")
  public BeforeConvertCallback<Object> zeabayGenericTsidBeforeConvertCallback(
      TsidIdGenerator tsidIdGenerator) {
    Map<Class<?>, Optional<Field>> idFieldCache = new ConcurrentHashMap<>();

    return (entity, table) -> {
      if (entity instanceof BaseEntity) {
        return Mono.just(entity);
      }

      Class<?> clazz = entity.getClass();

      Optional<Field> idFieldOpt =
          idFieldCache.computeIfAbsent(
              clazz,
              c ->
                  Arrays.stream(c.getDeclaredFields())
                      .filter(f -> f.isAnnotationPresent(Id.class) && f.getType() == Long.class)
                      .peek(f -> f.setAccessible(true))
                      .findFirst());

      idFieldOpt.ifPresent(
          field -> {
            try {
              if (field.get(entity) == null) {
                field.set(entity, tsidIdGenerator.newLongId());
              }
            } catch (IllegalAccessException ex) {
              log.warn(
                  "Could not assign TSID to field '{}' on {}: {}",
                  field.getName(),
                  clazz.getSimpleName(),
                  ex.getMessage());
            }
          });

      return Mono.just(entity);
    };
  }
}
