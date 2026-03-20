package com.zeabay.common.keycloak.client;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.zeabay.common.api.exception.BusinessException;
import com.zeabay.common.api.exception.ErrorCode;
import com.zeabay.common.keycloak.config.KeycloakProperties;
import com.zeabay.common.keycloak.dto.KeycloakRegistrationRequest;
import com.zeabay.common.keycloak.dto.KeycloakTokenRequest;
import com.zeabay.common.keycloak.dto.ZeabayTokenResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@RequiredArgsConstructor
public class ZeabayKeycloakClient {

  private final KeycloakProperties properties;
  private final Keycloak keycloakAdminClient;
  private final WebClient webClient;

  public Mono<String> registerUser(KeycloakRegistrationRequest request) {
    return Mono.fromCallable(
            () -> {
              log.info("Creating user account in Keycloak: {}", request.email());
              UserRepresentation user = mapToUserRepresentation(request);

              try (Response response =
                  keycloakAdminClient.realm(properties.realm()).users().create(user)) {
                if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
                  String location = response.getLocation().toString();
                  return location.substring(location.lastIndexOf('/') + 1);
                }

                String error =
                    response.hasEntity() ? response.readEntity(String.class) : "Keycloak Error";
                log.error("Keycloak registration failed for {}: {}", request.email(), error);

                if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                  throw new BusinessException(
                      ErrorCode.USER_ALREADY_EXISTS, "Email is already registered");
                }
                throw new RuntimeException("Identity Provider Error: " + error);
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<ZeabayTokenResponse> loginUser(KeycloakTokenRequest request) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("client_id", properties.resource());
    formData.add("client_secret", properties.credentials().secret());
    formData.add("grant_type", "password");
    formData.add("username", request.usernameOrEmail());
    formData.add("password", request.password());

    return postToTokenEndpoint(formData)
        .doOnError(
            e ->
                log.error(
                    "Error during Keycloak login for {}: {}",
                    request.usernameOrEmail(),
                    e.getMessage()));
  }

  public Mono<ZeabayTokenResponse> refreshToken(String refreshToken) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("client_id", properties.resource());
    formData.add("client_secret", properties.credentials().secret());
    formData.add("grant_type", "refresh_token");
    formData.add("refresh_token", refreshToken);

    return postToTokenEndpoint(formData)
        .doOnError(e -> log.error("Error during token refresh: {}", e.getMessage()));
  }

  public Mono<Void> setEmailVerified(String keycloakId, boolean verified) {
    return Mono.fromCallable(
            () -> {
              var userResource =
                  keycloakAdminClient.realm(properties.realm()).users().get(keycloakId);
              var representation = userResource.toRepresentation();
              representation.setEmailVerified(verified);
              userResource.update(representation);
              log.info("Set emailVerified={} for Keycloak user {}", verified, keycloakId);
              return (Void) null;
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<Void> logout(String keycloakId) {
    return Mono.fromCallable(
            () -> {
              keycloakAdminClient.realm(properties.realm()).users().get(keycloakId).logout();
              log.info("Logged out Keycloak user {}", keycloakId);
              return (Void) null;
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  /**
   * Assigns a realm-level role to the given Keycloak user.
   *
   * @param keycloakId Keycloak user UUID
   * @param roleName Realm role name (e.g. {@code "user"}, {@code "admin"})
   */
  public Mono<Void> assignRealmRole(String keycloakId, String roleName) {
    return Mono.fromCallable(
            () -> {
              var roleRep =
                  keycloakAdminClient
                      .realm(properties.realm())
                      .roles()
                      .get(roleName)
                      .toRepresentation();
              keycloakAdminClient
                  .realm(properties.realm())
                  .users()
                  .get(keycloakId)
                  .roles()
                  .realmLevel()
                  .add(List.of(roleRep));
              log.info("Assigned realm role '{}' to Keycloak user {}", roleName, keycloakId);
              return (Void) null;
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<Void> deleteUser(String keycloakId) {
    return Mono.fromCallable(
            () -> {
              keycloakAdminClient.realm(properties.realm()).users().get(keycloakId).remove();
              log.warn("Compensating action: deleted Keycloak user {}", keycloakId);
              return (Void) null;
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  private Mono<ZeabayTokenResponse> postToTokenEndpoint(MultiValueMap<String, String> formData) {
    String tokenUrl =
        properties.authServerUrl()
            + "/realms/"
            + properties.realm()
            + "/protocol/openid-connect/token";
    return webClient
        .post()
        .uri(tokenUrl)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters.fromFormData(formData))
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            clientResponse ->
                clientResponse
                    .bodyToMono(String.class)
                    .flatMap(error -> Mono.error(new RuntimeException("Auth Failed: " + error))))
        .bodyToMono(ZeabayTokenResponse.class);
  }

  private UserRepresentation mapToUserRepresentation(KeycloakRegistrationRequest request) {
    UserRepresentation user = new UserRepresentation();
    user.setUsername(request.username());
    user.setEmail(request.email());
    user.setEnabled(true);
    user.setEmailVerified(false);

    CredentialRepresentation credential = new CredentialRepresentation();
    credential.setType(CredentialRepresentation.PASSWORD);
    credential.setValue(request.password());
    credential.setTemporary(false);

    user.setCredentials(List.of(credential));
    return user;
  }
}
