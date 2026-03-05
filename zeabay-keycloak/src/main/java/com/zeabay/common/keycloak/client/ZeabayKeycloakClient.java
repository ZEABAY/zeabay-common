package com.zeabay.common.keycloak.client;

import com.zeabay.common.keycloak.config.KeycloakProperties;
import com.zeabay.common.keycloak.dto.KeycloakRegistrationRequest;
import com.zeabay.common.keycloak.dto.KeycloakTokenRequest;
import com.zeabay.common.keycloak.dto.KeycloakTokenResponse;
import com.zeabay.common.logging.Loggable;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Loggable
@RequiredArgsConstructor
public class ZeabayKeycloakClient {

  private final KeycloakProperties properties;
  private final Keycloak keycloakAdminClient;
  private final WebClient webClient = WebClient.builder().build();

  public Mono<String> registerUser(KeycloakRegistrationRequest request) {
    return Mono.fromCallable(
            () -> {
              log.info("Registering user in Keycloak via Admin SDK: {}", request.email());

              UserRepresentation user = new UserRepresentation();
              user.setUsername(request.username());
              user.setEmail(request.email());
              user.setEnabled(true);

              CredentialRepresentation credential = new CredentialRepresentation();
              credential.setType(CredentialRepresentation.PASSWORD);
              credential.setValue(request.password());
              credential.setTemporary(false);

              user.setCredentials(Collections.singletonList(credential));

              try (Response response =
                  keycloakAdminClient.realm(properties.realm()).users().create(user)) {

                if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
                  String location = response.getLocation().toString();
                  String keycloakId = location.substring(location.lastIndexOf('/') + 1);

                  log.info(
                      "Successfully registered user {} with Keycloak ID {}",
                      request.username(),
                      keycloakId);
                  return keycloakId;
                } else {
                  String errorMessage =
                      response.hasEntity() ? response.readEntity(String.class) : "Unknown error";
                  log.error(
                      "Failed to register user. Status: {}, Error: {}",
                      response.getStatus(),
                      errorMessage);
                  throw new RuntimeException("Keycloak Registration Error: " + errorMessage);
                }
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<KeycloakTokenResponse> loginUser(KeycloakTokenRequest request) {
    String tokenUrl =
        String.format(
            "%s/realms/%s/protocol/openid-connect/token",
            properties.authServerUrl(), properties.realm());

    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("client_id", properties.resource());
    formData.add("client_secret", properties.credentials().secret());
    formData.add("grant_type", "password");
    formData.add("username", request.usernameOrEmail());
    formData.add("password", request.password());

    return webClient
        .post()
        .uri(tokenUrl)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters.fromFormData(formData))
        .retrieve()
        .bodyToMono(Map.class)
        .map(
            response ->
                new KeycloakTokenResponse(
                    (String) response.get("access_token"),
                    (String) response.get("refresh_token"),
                    (Integer) response.get("expires_in")))
        .doOnError(
            e ->
                log.error(
                    "Error during Keycloak login for {}: {}",
                    request.usernameOrEmail(),
                    e.getMessage()));
  }
}
