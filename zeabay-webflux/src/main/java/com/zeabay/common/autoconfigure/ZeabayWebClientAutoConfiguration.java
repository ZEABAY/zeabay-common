package com.zeabay.common.autoconfigure;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Autoconfigures a shared {@link WebClient} bean with trace ID propagation.
 *
 * <p>If a {@code zeabayTraceIdWebClientFilter} filter is available, it is wired into the WebClient
 * builder automatically.
 */
@AutoConfiguration
@ConditionalOnClass(WebClient.class)
@AutoConfigureAfter(
    name =
        "org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration")
public class ZeabayWebClientAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(WebClient.class)
  public WebClient zeabayWebClient(
      ObjectProvider<WebClient.Builder> builderProvider,
      ObjectProvider<ExchangeFilterFunction> traceIdFilterProvider) {

    WebClient.Builder builder = builderProvider.getIfAvailable(WebClient::builder);
    ExchangeFilterFunction filter = traceIdFilterProvider.getIfAvailable();
    return filter != null ? builder.filter(filter).build() : builder.build();
  }
}
