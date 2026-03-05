package com.zeabay.common.autoconfigure;

import com.zeabay.common.r2dbc.ZeabayReactiveAuditorAware;
import com.zeabay.common.security.ZeabaySecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

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
   */
  @Bean
  @ConditionalOnMissingBean(SecurityWebFilterChain.class)
  public SecurityWebFilterChain zeabayDefaultSecurityFilterChain(
      ServerHttpSecurity http, CorsConfigurationSource zeabayCorsConfigurationSource) {
    return http.cors(cors -> cors.configurationSource(zeabayCorsConfigurationSource))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(
            auth ->
                auth.pathMatchers("/actuator/**", "/actuator/health/**")
                    .permitAll()
                    .anyExchange()
                    .authenticated())
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .build();
  }

  /** Security-aware auditor — overrides the "system" fallback from zeabay-r2dbc. */
  @Bean
  public ReactiveAuditorAware<String> zeabayReactiveAuditorAware() {
    return new ZeabayReactiveAuditorAware();
  }
}
