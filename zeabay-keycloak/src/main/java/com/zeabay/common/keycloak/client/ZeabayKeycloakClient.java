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

/**
 * Reactive client for Keycloak user management and token operations.
 *
 * <p>Wraps the blocking Keycloak Admin SDK calls with {@code Schedulers.boundedElastic()} and uses
 * {@link WebClient} for the OpenID Connect token endpoint.
 */
@Slf4j
@RequiredArgsConstructor
public class ZeabayKeycloakClient {

  private final KeycloakProperties properties;
  private final Keycloak keycloakAdminClient;
  private final WebClient webClient;

  /**
   * Registers a new user in Keycloak and returns the assigned Keycloak user ID.
   *
   * @param request registration details (username, email, password)
   * @return a {@link Mono} emitting the Keycloak user UUID
   * @throws BusinessException if the email is already registered (409 Conflict)
   */
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
                throw new BusinessException(
                    ErrorCode.IDENTITY_PROVIDER_ERROR, "Keycloak user creation failed: " + error);
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  /**
   * Authenticates a user via the Keycloak token endpoint using the password grant.
   *
   * @param request login credentials
   * @return a {@link Mono} emitting the access/refresh token pair
   */
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

  /**
   * Exchanges a refresh token for a new access/refresh token pair.
   *
   * @param refreshToken the current refresh token
   * @return a {@link Mono} emitting the new token pair
   */
  public Mono<ZeabayTokenResponse> refreshToken(String refreshToken) {
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("client_id", properties.resource());
    formData.add("client_secret", properties.credentials().secret());
    formData.add("grant_type", "refresh_token");
    formData.add("refresh_token", refreshToken);

    return postToTokenEndpoint(formData)
        .doOnError(e -> log.error("Error during token refresh: {}", e.getMessage()));
  }

  /**
   * Updates the email verification status of a Keycloak user.
   *
   * @param keycloakId Keycloak user UUID
   * @param verified {@code true} to mark the email as verified
   * @return a {@link Mono} that completes when the update is done
   */
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

  /**
   * Terminates all active sessions for a Keycloak user.
   *
   * @param keycloakId Keycloak user UUID
   * @return a {@link Mono} that completes when the logout is done
   */
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
   * Resets the password of a Keycloak user.
   *
   * @param keycloakId Keycloak user UUID
   * @param newPassword the new password (not temporary)
   * @return a {@link Mono} that completes when the password is reset
   */
  public Mono<Void> resetPassword(String keycloakId, String newPassword) {
    return Mono.fromCallable(
            () -> {
              CredentialRepresentation credential = new CredentialRepresentation();
              credential.setType(CredentialRepresentation.PASSWORD);
              credential.setValue(newPassword);
              credential.setTemporary(false);

              keycloakAdminClient
                  .realm(properties.realm())
                  .users()
                  .get(keycloakId)
                  .resetPassword(credential);
              log.info("Reset password for Keycloak user {}", keycloakId);
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

  /**
   * Deletes a Keycloak user. Typically used as a compensating action during saga rollbacks.
   *
   * @param keycloakId Keycloak user UUID
   * @return a {@link Mono} that completes when the user is deleted
   */
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
                    .flatMap(
                        error ->
                            Mono.error(
                                new BusinessException(
                                    ErrorCode.INVALID_CREDENTIALS,
                                    "Keycloak authentication failed: " + error))))
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
