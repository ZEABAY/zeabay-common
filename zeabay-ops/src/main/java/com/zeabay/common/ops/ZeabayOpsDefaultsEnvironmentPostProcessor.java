package com.zeabay.common.ops;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public final class ZeabayOpsDefaultsEnvironmentPostProcessor
    implements EnvironmentPostProcessor, Ordered {

  private static final String PROPERTY_SOURCE_NAME = "zeabayOpsDefaults";

  private static void putIfMissing(
      ConfigurableEnvironment env, Map<String, Object> target, String key, String value) {
    if (env.getProperty(key) == null) {
      target.put(key, value);
    }
  }

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {

    Map<String, Object> defaults = new LinkedHashMap<>();

    putIfMissing(environment, defaults, "management.endpoint.health.probes.enabled", "true");
    putIfMissing(environment, defaults, "management.endpoint.health.show-details", "never");

    putIfMissing(environment, defaults, "management.endpoint.livenessstate.enabled", "true");
    putIfMissing(environment, defaults, "management.endpoint.readinessstate.enabled", "true");

    putIfMissing(environment, defaults, "management.info.build.enabled", "true");
    putIfMissing(environment, defaults, "management.endpoint.prometheus.enabled", "true");

    boolean isProd = environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod"));

    if (isProd) {
      putIfMissing(environment, defaults, "management.endpoints.web.exposure.include", "health");
    } else {
      putIfMissing(
          environment,
          defaults,
          "management.endpoints.web.exposure.include",
          "health,info,metrics,prometheus");
    }

    if (!defaults.isEmpty() && environment.getPropertySources().get(PROPERTY_SOURCE_NAME) == null) {
      environment
          .getPropertySources()
          .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 50;
  }
}
