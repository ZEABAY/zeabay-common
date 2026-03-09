package com.zeabay.common.ops;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;

/**
 * Spring Boot ortam ayarlarını, uygulama başlamadan önce varsayılan değerlerle doldurur.
 *
 * <p>Actuator (health, metrics, prometheus) ayarlarını otomatik yapar; servislerin
 * application.yml'de tekrar yazmasına gerek kalmaz. İstenirse application.yml ile override
 * edilebilir.
 *
 * <p>Prod'da sadece health expose edilir (güvenlik). Prometheus/metrics için servis
 * application-prod.yml'de {@code management.endpoints.web.exposure.include} veya {@code
 * management.server.port} ile ayrı port kullanarak açmalıdır.
 */
public final class ZeabayOpsDefaultsEnvironmentPostProcessor
    implements EnvironmentPostProcessor, Ordered {

  private static final String PROPERTY_SOURCE_NAME = "zeabayOpsDefaults";

  /** Sadece property tanımlı değilse varsayılan değeri ekler; mevcut değere dokunmaz. */
  private static void putIfMissing(
      ConfigurableEnvironment env, Map<String, Object> target, String key, String value) {
    if (env.getProperty(key) == null) {
      target.put(key, value);
    }
  }

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    // TODO: Prod'da monitoring erişimi için management.server.port ile ayrı port kullanılacak.
    // exposure.include prometheus
    Map<String, Object> defaults = new LinkedHashMap<>();

    // Kubernetes liveness/readiness probe'ları için health endpoint
    putIfMissing(environment, defaults, "management.endpoint.health.probes.enabled", "true");
    putIfMissing(environment, defaults, "management.endpoint.health.show-details", "never");

    // Liveness/readiness state endpoint'leri (K8s ile uyumlu)
    putIfMissing(environment, defaults, "management.endpoint.livenessstate.enabled", "true");
    putIfMissing(environment, defaults, "management.endpoint.readinessstate.enabled", "true");

    // Build info ve Prometheus endpoint'leri etkin
    putIfMissing(environment, defaults, "management.info.build.enabled", "true");
    putIfMissing(environment, defaults, "management.endpoint.prometheus.enabled", "true");

    // Prod: sadece health (güvenlik). Diğer ortamlarda: health, info, metrics, prometheus
    boolean isProd = environment.acceptsProfiles(Profiles.of("prod"));
    if (isProd) {
      putIfMissing(environment, defaults, "management.endpoints.web.exposure.include", "health");
    } else {
      putIfMissing(
          environment,
          defaults,
          "management.endpoints.web.exposure.include",
          "health,info,metrics,prometheus");
    }

    // Varsayılanları en düşük öncelikle ekle (application.yml override edebilir)
    if (!defaults.isEmpty() && environment.getPropertySources().get(PROPERTY_SOURCE_NAME) == null) {
      environment
          .getPropertySources()
          .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }
  }

  /** Erken çalışması için yüksek öncelik. */
  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 50;
  }
}
