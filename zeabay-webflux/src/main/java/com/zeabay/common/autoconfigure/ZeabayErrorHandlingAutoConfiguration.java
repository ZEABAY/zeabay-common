package com.zeabay.common.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.zeabay.common.web.ZeabayGlobalExceptionHandler;

@AutoConfiguration
public class ZeabayErrorHandlingAutoConfiguration {

  @Bean
  ZeabayGlobalExceptionHandler zeabayGlobalExceptionHandler() {
    return new ZeabayGlobalExceptionHandler();
  }
}
