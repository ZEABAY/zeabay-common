package com.zeabay.common.autoconfigure;

import com.zeabay.common.tsid.TsidIdGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class ZeabayCommonAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public TsidIdGenerator tsidIdGenerator() {
    return new TsidIdGenerator();
  }
}
