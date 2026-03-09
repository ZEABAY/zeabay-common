package com.zeabay.common.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ZeabayTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("expires_in") Integer expiresIn) {}
