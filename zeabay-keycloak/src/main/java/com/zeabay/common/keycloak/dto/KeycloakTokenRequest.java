package com.zeabay.common.keycloak.dto;

import lombok.Builder;

@Builder
public record KeycloakTokenRequest(String usernameOrEmail, String password) {}
