package com.zeabay.common.autoconfigure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@ConditionalOnProperty(
    name = "springdoc.api-docs.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ZeabayOpenApiAutoConfiguration {

  @Bean
  public OpenAPI zeabayOpenApi(
      @Value("${spring.application.name:Zeabay Application}") String appName,
      @Value("${spring.application.version:1.0.0}") String appVersion,
      @Value("${zeabay.openapi.server-url:}") String serverUrl,
      @Value("${zeabay.openapi.server-description:API Server}") String serverDescription) {

    SecurityScheme bearer =
        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT");

    Info info =
        new Info()
            .title(appName + " API")
            .description("Auto-generated API documentation for " + appName)
            .version(appVersion);

    OpenAPI openApi =
        new OpenAPI()
            .info(info)
            .components(new Components().addSecuritySchemes("bearerAuth", bearer));

    // If a server URL is explicitly configured, add it. This overrides the auto-detected URL.
    if (serverUrl != null && !serverUrl.isBlank()) {
      openApi.addServersItem(new Server().url(serverUrl).description(serverDescription));
    }

    return openApi;
  }
}
