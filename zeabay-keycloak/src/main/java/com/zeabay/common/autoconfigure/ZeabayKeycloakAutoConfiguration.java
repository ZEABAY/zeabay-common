package com.zeabay.common.autoconfigure;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

import com.zeabay.common.keycloak.client.ZeabayKeycloakClient;
import com.zeabay.common.keycloak.config.KeycloakProperties;

/**
 * Autoconfigures Keycloak Admin SDK and the reactive {@link ZeabayKeycloakClient}.
 *
 * <p>Activates only when {@code keycloak.auth-server-url} is set, so services that do not need
 * Keycloak integration remain unaffected.
 */
@AutoConfiguration
@AutoConfigureAfter(name = "com.zeabay.common.autoconfigure.ZeabayWebClientAutoConfiguration")
@EnableConfigurationProperties(KeycloakProperties.class)
@ConditionalOnProperty(prefix = "keycloak", name = "auth-server-url")
public class ZeabayKeycloakAutoConfiguration {

  /** Creates a Keycloak Admin SDK client using master-realm admin credentials. */
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

  /** Creates the reactive Keycloak client used for user registration, login, and management. */
  @Bean
  public ZeabayKeycloakClient zeabayKeycloakClient(
      Keycloak keycloakAdminClient, KeycloakProperties properties) {
    WebClient webClient = WebClient.builder().build();
    return new ZeabayKeycloakClient(properties, keycloakAdminClient, webClient);
  }
}
