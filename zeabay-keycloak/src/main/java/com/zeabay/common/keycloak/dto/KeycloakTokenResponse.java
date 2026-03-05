package com.zeabay.common.keycloak.dto;

public record KeycloakTokenResponse(String accessToken, String refreshToken, Integer expiresIn) {}
