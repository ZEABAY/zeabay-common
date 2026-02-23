package com.zeabay.common.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
@ConditionalOnClass(WebClient.class)
@AutoConfigureAfter(
    name =
        "org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration")
public class ZeabayWebClientAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
  }

  @Bean
  public WebClient webClient(
      WebClient.Builder builder, ExchangeFilterFunction zeabayTraceIdWebClientFilter) {
    return builder.filter(zeabayTraceIdWebClientFilter).build();
  }
}
