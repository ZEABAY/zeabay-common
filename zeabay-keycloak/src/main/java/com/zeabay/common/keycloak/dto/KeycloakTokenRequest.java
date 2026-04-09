package com.zeabay.common.keycloak.dto;

import lombok.Builder;

/** Credentials for authenticating a user via the Keycloak token endpoint (password grant). */
@Builder
public record KeycloakTokenRequest(String usernameOrEmail, String password) {}
