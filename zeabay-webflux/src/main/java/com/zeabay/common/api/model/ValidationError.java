package com.zeabay.common.api.model;

import lombok.Builder;

@Builder
public record ValidationError(String field, String message) {}
