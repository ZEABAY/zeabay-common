package com.zeabay.common.keycloak.dto;

import lombok.Builder;

@Builder
public record KeycloakRegistrationRequest(String username, String email, String password) {}
