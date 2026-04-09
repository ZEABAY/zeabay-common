package com.zeabay.common.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import com.zeabay.common.r2dbc.ZeabayReactiveAuditorAware;
import com.zeabay.common.security.ZeabaySecurityProperties;

import reactor.core.publisher.Flux;

/**
 * Autoconfigures WebFlux security with JWT/OAuth2 resource server, CORS, and Keycloak role mapping.
 *
 * <p>Public paths from {@link ZeabaySecurityProperties} and actuator endpoints are always
 * permitted. When a {@link ReactiveJwtDecoder} bean is present, OAuth2 resource server is
 * configured automatically.
 */
// beforeName (string) instead of before (class ref) — ZeabayR2dbcAuditingAutoConfiguration is
// an optional dependency; using the class literal would cause ClassNotFoundException on the
// gateway.
@AutoConfiguration(
    beforeName = "com.zeabay.common.autoconfigure.ZeabayR2dbcAuditingAutoConfiguration")
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@EnableConfigurationProperties(ZeabaySecurityProperties.class)
public class ZeabaySecurityAutoConfiguration {

  /** Builds a URL-based CORS configuration source from the {@link ZeabaySecurityProperties}. */
  @Bean
  public CorsConfigurationSource zeabayCorsConfigurationSource(ZeabaySecurityProperties props) {
    ZeabaySecurityProperties.Cors cors = props.getCors();
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    if (!cors.isEnabled()) {
      return source; // Returns empty source, effectively disabling CORS parsing for this service
    }

    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(cors.getAllowedOrigins());
    config.setAllowedMethods(cors.getAllowedMethods());
    config.setAllowedHeaders(cors.getAllowedHeaders());
    config.setExposedHeaders(cors.getExposedHeaders());
    config.setAllowCredentials(cors.isAllowCredentials());
    config.setMaxAge(cors.getMaxAge().getSeconds());
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  /**
   * Builds the default security filter chain. Public paths, actuator, CORS, CSRF, JWT are
   * configured here.
   */
  @Bean
  @ConditionalOnMissingBean(SecurityWebFilterChain.class)
  public SecurityWebFilterChain zeabayDefaultSecurityFilterChain(
      ServerHttpSecurity http,
      ZeabaySecurityProperties securityProps,
      CorsConfigurationSource zeabayCorsConfigurationSource,
      @Autowired(required = false) ReactiveJwtDecoder jwtDecoder,
      @Autowired(required = false) ReactiveJwtAuthenticationConverter jwtAuthenticationConverter) {

    List<String> allPublic = new ArrayList<>();
    allPublic.add("/actuator/**");
    allPublic.add("/actuator/health/**");
    allPublic.addAll(securityProps.getPublicPaths());

    if (securityProps.getCors().isEnabled()) {
      http.cors(cors -> cors.configurationSource(zeabayCorsConfigurationSource));
    } else {
      http.cors(ServerHttpSecurity.CorsSpec::disable);
    }

    ServerHttpSecurity serverHttp =
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(
                exchange ->
                    exchange
                        .pathMatchers(allPublic.toArray(new String[0]))
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
   * Maps Keycloak {@code realm_access.roles} from the JWT into Spring Security {@code ROLE_*}
   * authorities.
   */
  @Bean
  @ConditionalOnMissingBean(ReactiveJwtAuthenticationConverter.class)
  @ConditionalOnBean(ReactiveJwtDecoder.class)
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

  /**
   * Registers the security-aware auditor that reads the authenticated principal name. Only active
   * when {@code zeabay-r2dbc} is on the classpath.
   */
  // Only registered when zeabay-r2dbc is on the classpath (e.g. auth-service).
  // Skipped on the gateway which does not have a database.
  @Bean
  @ConditionalOnClass(name = "com.zeabay.common.r2dbc.ZeabayReactiveAuditorAware")
  public ReactiveAuditorAware<String> zeabayReactiveAuditorAware() {
    return new ZeabayReactiveAuditorAware();
  }
}
