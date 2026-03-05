package com.zeabay.common.security;

import java.time.Duration;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties for zeabay-security.
 *
 * <p>Override in each service's {@code application.yml}:
 *
 * <pre>
 * zeabay:
 *   security:
 *     jwt:
 *       secret: "your-256-bit-secret"
 *       access-token-expiry: 15m
 *       refresh-token-expiry: 7d
 *     cors:
 *       allowed-origins:
 *         - "http://localhost:3000"
 *         - "https://pulse.zeabay.com"
 *       allowed-methods:
 *         - GET
 *         - POST
 *         - PUT
 *         - DELETE
 *         - OPTIONS
 *       allowed-headers:
 *         - "*"
 *       allow-credentials: true
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "zeabay.security")
public class ZeabaySecurityProperties {

  private Jwt jwt = new Jwt();
  private Cors cors = new Cors();

  @Data
  public static class Jwt {
    /** HMAC-SHA256 secret key (min 32 chars for HS256 safety). */
    private String secret = "change-me-in-production-min-32-chars!!";

    /** Access token lifetime. Default: 15 minutes. yml: {@code access-token-expiry: 15m} */
    private Duration accessTokenExpiry = Duration.ofMinutes(15);

    /** Refresh token lifetime. Default: 7 days. yml: {@code refresh-token-expiry: 7d} */
    private Duration refreshTokenExpiry = Duration.ofDays(7);
  }

  @Data
  public static class Cors {
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
