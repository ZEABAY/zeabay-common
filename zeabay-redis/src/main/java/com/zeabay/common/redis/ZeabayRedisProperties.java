package com.zeabay.common.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Configuration properties for zeabay-redis.
 *
 * <p>Override in each service's {@code application.yml}:
 *
 * <pre>
 * zeabay:
 *   redis:
 *     host: localhost
 *     port: 6379
 *     prefix: zeabay
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "zeabay.redis")
public class ZeabayRedisProperties {

  private String prefix = "zeabay";
}
