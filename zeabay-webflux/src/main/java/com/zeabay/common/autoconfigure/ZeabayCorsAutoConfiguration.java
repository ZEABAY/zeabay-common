package com.zeabay.common.autoconfigure;

import static org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import com.zeabay.common.constant.ZeabayConstants;

/**
 * Fallback CORS configuration for reactive WebFlux applications without Spring Security.
 *
 * <p>When {@code SecurityWebFilterChain} is on the classpath, CORS is managed by {@code
 * ZeabaySecurityAutoConfiguration} instead, and this configuration is skipped.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = REACTIVE)
@ConditionalOnMissingClass("org.springframework.security.web.server.SecurityWebFilterChain")
public class ZeabayCorsAutoConfiguration {

  @Bean
  // Intentionally suppressing the security warning regarding permissive CORS wildcards.
  // This configuration is intended for development/Sprint 0 purposes only.
  // Production restrictions will be enforced via the API Gateway or external configuration.
  @SuppressWarnings("java:S5122")
  public CorsWebFilter corsWebFilter() {
    CorsConfiguration config = new CorsConfiguration();

    // Default secure but permissive standard for dev/Sprint 0
    config.setAllowedOriginPatterns(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of(ZeabayConstants.TRACE_ID_HEADER, "traceparent"));
    config.setAllowCredentials(false);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);

    return new CorsWebFilter(source);
  }
}
