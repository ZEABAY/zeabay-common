package com.zeabay.common.security;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Typed configuration properties for zeabay-security.
 *
 * <p>Override in each service's {@code application.yml}:
 *
 * <pre>
 * zeabay:
 * security:
 * public-paths:
 *   - /api/v1/auth/register
 *   - /api/v1/auth/login
 * cors:
 * allowed-origins:
 * - "http://localhost:3000"
 * - "https://myapp.zeabay.com"
 * allowed-methods:
 * - GET
 * - POST
 * - PUT
 * - DELETE
 * - OPTIONS
 * allowed-headers:
 * - "*"
 * allow-credentials: true
 * max-age: 1h
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "zeabay.security")
public class ZeabaySecurityProperties {

  /** Paths that do not require authentication. */
  private List<String> publicPaths = new ArrayList<>();

  private Cors cors = new Cors();

  @Data
  public static class Cors {
    /** Whether CORS configuration is enabled for this service. Default false (rely on Gateway). */
    private boolean enabled = false;

    private List<String> allowedOrigins = List.of("http://localhost:3000");
    private List<String> allowedMethods =
        List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");
    private List<String> allowedHeaders = List.of("*");
    private List<String> exposedHeaders = List.of("X-Trace-Id", "Authorization");
    private boolean allowCredentials = true;

    /** Pre-flight cache duration. Default: 1 hour. yml: {@code max-age: 1h} */
    private Duration maxAge = Duration.ofHours(1);
  }
}
