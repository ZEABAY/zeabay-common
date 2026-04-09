package com.zeabay.common.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** OAuth 2.0 token response returned by the Keycloak token endpoint. */
public record ZeabayTokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("expires_in") Integer expiresIn,
    @JsonProperty("refresh_expires_in") Integer refreshExpiresIn) {}
