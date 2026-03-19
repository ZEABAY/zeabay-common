package com.zeabay.common.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.zeabay.common.redis.ZeabayRedisProperties;

@AutoConfiguration
@ConditionalOnClass(ReactiveRedisTemplate.class)
@EnableConfigurationProperties(ZeabayRedisProperties.class)
public class ZeabayRedisAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(ReactiveRedisTemplate.class)
  public ReactiveRedisTemplate<String, String> zeabayReactiveRedisTemplate(
      ReactiveRedisConnectionFactory connectionFactory) {
    StringRedisSerializer serializer = new StringRedisSerializer();
    RedisSerializationContext<String, String> context =
        RedisSerializationContext.<String, String>newSerializationContext(serializer)
            .key(serializer)
            .value(serializer)
            .hashKey(serializer)
            .hashValue(serializer)
            .build();
    return new ReactiveRedisTemplate<>(connectionFactory, context);
  }
}
