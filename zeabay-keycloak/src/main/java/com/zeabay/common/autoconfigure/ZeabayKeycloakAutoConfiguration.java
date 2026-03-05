package com.zeabay.common.autoconfigure;

import com.zeabay.common.keycloak.client.ZeabayKeycloakClient;
import com.zeabay.common.keycloak.config.KeycloakProperties;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(KeycloakProperties.class)
@ConditionalOnProperty(prefix = "keycloak", name = "auth-server-url")
public class ZeabayKeycloakAutoConfiguration {

  @Bean
  public Keycloak keycloakAdminClient(KeycloakProperties properties) {
    return KeycloakBuilder.builder()
        .serverUrl(properties.authServerUrl())
        .realm("master")
        .grantType("password")
        .clientId("admin-cli")
        .username(properties.admin().username())
        .password(properties.admin().password())
        .build();
  }

  @Bean
  public ZeabayKeycloakClient zeabayKeycloakClient(
      Keycloak keycloakAdminClient, KeycloakProperties properties) {
    return new ZeabayKeycloakClient(properties, keycloakAdminClient);
  }
}
