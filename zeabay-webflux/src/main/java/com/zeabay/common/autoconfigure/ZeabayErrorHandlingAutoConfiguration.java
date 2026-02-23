package com.zeabay.common.autoconfigure;

import com.zeabay.common.web.ZeabayGlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ZeabayErrorHandlingAutoConfiguration {

  @Bean
  ZeabayGlobalExceptionHandler zeabayGlobalExceptionHandler() {
    return new ZeabayGlobalExceptionHandler();
  }
}
