package com.zeabay.common.autoconfigure;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
public class ZeabayOpenApiAutoConfiguration {

  @Bean
  public OpenAPI zeabayOpenApi(
      @Value("${spring.application.name:Zeabay Application}") String appName,
      @Value("${spring.application.version:1.0.0}") String appVersion) {

    SecurityScheme bearer =
        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT");

    Info info =
        new Info()
            .title(appName + " API")
            .description("Auto-generated API documentation for " + appName)
            .version(appVersion);

    return new OpenAPI()
        .info(info)
        .components(new Components().addSecuritySchemes("bearerAuth", bearer))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }
}
