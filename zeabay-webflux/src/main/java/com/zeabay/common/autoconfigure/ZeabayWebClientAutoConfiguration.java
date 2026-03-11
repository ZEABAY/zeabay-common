package com.zeabay.common.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
  public WebClient zeabayWebClient(
      ObjectProvider<WebClient.Builder> builderProvider,
      ExchangeFilterFunction zeabayTraceIdWebClientFilter) {

    WebClient.Builder builder = builderProvider.getIfAvailable(WebClient::builder);
    return builder.filter(zeabayTraceIdWebClientFilter).build();
  }
}
