package com.zeabay.common.autoconfigure;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;

import com.zeabay.common.r2dbc.BaseEntity;
import com.zeabay.common.tsid.TsidGenerator;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@AutoConfiguration
@EnableR2dbcAuditing(auditorAwareRef = "zeabayReactiveAuditorAware")
public class ZeabayR2dbcAuditingAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = "zeabayReactiveAuditorAware")
  public ReactiveAuditorAware<String> zeabayReactiveAuditorAware() {
    return () -> Mono.just("system");
  }

  @Bean
  @ConditionalOnMissingBean(name = "zeabayTsidBeforeConvertCallback")
  public BeforeConvertCallback<BaseEntity> zeabayTsidBeforeConvertCallback(
      TsidGenerator tsidGenerator) {
    return (entity, table) -> {
      if (entity.getId() == null) {
        entity.setId(tsidGenerator.newLongId());
      }
      return Mono.just(entity);
    };
  }

  @Bean
  @ConditionalOnMissingBean(name = "zeabayGenericTsidBeforeConvertCallback")
  public BeforeConvertCallback<Object> zeabayGenericTsidBeforeConvertCallback(
      TsidGenerator tsidGenerator) {
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
                field.set(entity, tsidGenerator.newLongId());
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
