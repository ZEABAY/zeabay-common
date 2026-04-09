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
 * Injects platform-wide default properties before the application context starts.
 *
 * <p>Configures Actuator endpoints (health, metrics, Prometheus) so each service does not need to
 * repeat these settings in {@code application.yml}. Individual services can still override any
 * value.
 *
 * <p>In production, only the health endpoint is exposed by default for security. Other environments
 * expose health, info, metrics, and Prometheus. Forward headers strategy is always set to {@code
 * native} since services run behind a gateway.
 */
public final class ZeabayOpsDefaultsEnvironmentPostProcessor
    implements EnvironmentPostProcessor, Ordered {

  private static final String PROPERTY_SOURCE_NAME = "zeabayOpsDefaults";
  private static final String FIXED_PROPERTY_SOURCE_NAME = "zeabayOpsFixed";

  /** Adds the default value only when the property is not already defined. */
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

    // Kubernetes liveness/readiness probes: health endpoint
    putIfMissing(environment, defaults, "management.endpoint.health.probes.enabled", "true");
    putIfMissing(environment, defaults, "management.endpoint.health.show-details", "never");

    // Liveness/readiness state endpoints (K8s compatible)
    putIfMissing(environment, defaults, "management.endpoint.livenessstate.enabled", "true");
    putIfMissing(environment, defaults, "management.endpoint.readinessstate.enabled", "true");

    // Build info and Prometheus endpoints enabled
    putIfMissing(environment, defaults, "management.info.build.enabled", "true");
    putIfMissing(environment, defaults, "management.endpoint.prometheus.enabled", "true");

    // Prod: health only (security). Other environments: health, info, metrics, prometheus
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

    // Fixed decisions: cannot be overridden (addFirst = highest priority)
    Map<String, Object> fixed = new LinkedHashMap<>();
    fixed.put("server.forward-headers-strategy", "native"); // Always behind gateway
    if (environment.getPropertySources().get(FIXED_PROPERTY_SOURCE_NAME) == null) {
      environment
          .getPropertySources()
          .addFirst(new MapPropertySource(FIXED_PROPERTY_SOURCE_NAME, fixed));
    }

    // Add defaults with lowest priority (application.yml can override)
    if (!defaults.isEmpty() && environment.getPropertySources().get(PROPERTY_SOURCE_NAME) == null) {
      environment
          .getPropertySources()
          .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }
  }

  /** High priority so defaults are injected early in the startup lifecycle. */
  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 50;
  }
}
