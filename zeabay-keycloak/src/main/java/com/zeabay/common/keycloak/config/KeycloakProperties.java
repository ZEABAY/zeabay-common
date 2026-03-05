package com.zeabay.common.keycloak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
    String authServerUrl, String realm, String resource, Credentials credentials, Admin admin) {
  public record Credentials(String secret) {}

  public record Admin(String username, String password) {}
}
