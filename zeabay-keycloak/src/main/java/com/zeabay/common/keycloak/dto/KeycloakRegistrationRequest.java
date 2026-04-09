package com.zeabay.common.keycloak.dto;

import lombok.Builder;

/** Data required to register a new user in Keycloak. */
@Builder
public record KeycloakRegistrationRequest(String username, String email, String password) {}
