package com.zeabay.common.logging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfigures the {@link LoggingAspect} bean.
 *
 * <p>Can be disabled by setting {@code zeabay.logging.enabled=false}.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "zeabay.logging.enabled", matchIfMissing = true)
public class ZeabayLoggingAutoConfiguration {

  @Bean
  public LoggingAspect loggingAspect() {
    return new LoggingAspect();
  }
}
