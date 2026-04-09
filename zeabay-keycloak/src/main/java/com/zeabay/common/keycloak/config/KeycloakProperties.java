package com.zeabay.common.keycloak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Keycloak integration (prefix: {@code keycloak}).
 *
 * <p>Includes server URL, realm, client resource, client secret, and admin credentials.
 */
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
    String authServerUrl, String realm, String resource, Credentials credentials, Admin admin) {
  public record Credentials(String secret) {}

  public record Admin(String username, String password) {}
}
