package com.zeabay.common.autoconfigure;

import com.zeabay.common.r2dbc.ZeabayReactiveAuditorAware;
import com.zeabay.common.security.ZeabaySecurityProperties;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Flux;

/** Autoconfigures CORS and Spring Security for WebFlux services. */
@AutoConfiguration
@EnableWebFluxSecurity
@EnableConfigurationProperties(ZeabaySecurityProperties.class)
public class ZeabaySecurityAutoConfiguration {

  /** CORS configuration source wired from yml-overridable {@link ZeabaySecurityProperties}. */
  @Bean
  public CorsConfigurationSource zeabayCorsConfigurationSource(ZeabaySecurityProperties props) {
    ZeabaySecurityProperties.Cors cors = props.getCors();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(cors.getAllowedOrigins());
    config.setAllowedMethods(
        cors.getAllowedMethods().stream().map(HttpMethod::valueOf).map(HttpMethod::name).toList());
    config.setAllowedHeaders(cors.getAllowedHeaders());
    config.setExposedHeaders(cors.getExposedHeaders());
    config.setAllowCredentials(cors.isAllowCredentials());
    config.setMaxAge(cors.getMaxAge().getSeconds());
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  /**
   * Default security filter chain. Services declare their own {@code @Bean SecurityWebFilterChain}
   * to override ({@code @ConditionalOnMissingBean}).
   *
   * <p>When {@code spring.security.oauth2.resourceserver.jwt} is configured, JWT Resource Server is
   * enabled automatically. Use {@link #zeabayJwtAuthenticationConverter()} for Keycloak
   * realm_access.roles mapping, or override with your own bean.
   */
  @Bean
  @ConditionalOnMissingBean(SecurityWebFilterChain.class)
  public SecurityWebFilterChain zeabayDefaultSecurityFilterChain(
      ServerHttpSecurity http,
      CorsConfigurationSource zeabayCorsConfigurationSource,
      @Autowired(required = false) JwtDecoder jwtDecoder,
      @Autowired(required = false) ReactiveJwtAuthenticationConverter jwtAuthenticationConverter) {

    ServerHttpSecurity serverHttp =
        http.cors(cors -> cors.configurationSource(zeabayCorsConfigurationSource))
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(
                exchange ->
                    exchange
                        .pathMatchers("/actuator/**", "/actuator/health/**")
                        .permitAll()
                        .anyExchange()
                        .authenticated())
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable);

    if (jwtDecoder != null) {
      serverHttp =
          serverHttp.oauth2ResourceServer(
              oauth2 ->
                  oauth2.jwt(
                      jwt ->
                          jwt.jwtAuthenticationConverter(
                              jwtAuthenticationConverter != null
                                  ? jwtAuthenticationConverter
                                  : zeabayJwtAuthenticationConverter())));
    }

    return serverHttp.build();
  }

  /**
   * Maps Keycloak's {@code realm_access.roles} claim to Spring Security authorities with {@code
   * ROLE_} prefix. Override with {@code @Bean ReactiveJwtAuthenticationConverter} to customize.
   */
  @Bean
  @ConditionalOnMissingBean(ReactiveJwtAuthenticationConverter.class)
  @ConditionalOnBean(JwtDecoder.class)
  public ReactiveJwtAuthenticationConverter zeabayJwtAuthenticationConverter() {
    var converter = new ReactiveJwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
          if (realmAccess == null) {
            return Flux.empty();
          }
          @SuppressWarnings("unchecked")
          List<String> roles = (List<String>) realmAccess.getOrDefault("roles", List.of());
          return Flux.fromIterable(roles)
              .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        });
    return converter;
  }

  /** Security-aware auditor — overrides the "system" fallback from zeabay-r2dbc. */
  @Bean
  public ReactiveAuditorAware<String> zeabayReactiveAuditorAware() {
    return new ZeabayReactiveAuditorAware();
  }
}
