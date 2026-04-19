package com.zeabay.common.keycloak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Keycloak integration (prefix: {@code keycloak}).
 *
 * <p>Includes server URL, realm, client resource, client secret, and admin credentials. Overrides
 * {@link #toString()} to prevent credential leakage in logs.
 */
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
    String authServerUrl, String realm, String resource, Credentials credentials, Admin admin) {

  public record Credentials(String secret) {
    @Override
    public String toString() {
      return "Credentials{secret=***}";
    }
  }

  public record Admin(String username, String password) {
    @Override
    public String toString() {
      return "Admin{username='%s', password=***}".formatted(username);
    }
  }

  @Override
  public String toString() {
    return "KeycloakProperties{authServerUrl='%s', realm='%s', resource='%s'}"
        .formatted(authServerUrl, realm, resource);
  }
}
