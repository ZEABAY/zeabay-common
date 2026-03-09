package com.zeabay.common.r2dbc;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

/**
 * Provides the current auditor (user) for @CreatedBy / @LastModifiedBy fields.
 *
 * <p>Reads the authenticated principal from Spring Security's reactive context. Falls back to
 * "system" when there is no authenticated user (e.g., background jobs).
 */
public class ZeabayReactiveAuditorAware implements ReactiveAuditorAware<String> {

  private static final String SYSTEM = "system";

  @Override
  @NonNull
  public Mono<String> getCurrentAuditor() {
    return ReactiveSecurityContextHolder.getContext()
        .mapNotNull(SecurityContext::getAuthentication)
        .filter(Authentication::isAuthenticated)
        .map(Authentication::getName)
        .defaultIfEmpty(SYSTEM);
  }
}
