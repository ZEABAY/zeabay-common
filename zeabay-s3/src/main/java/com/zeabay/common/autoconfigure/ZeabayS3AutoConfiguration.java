package com.zeabay.common.autoconfigure;

import java.net.URI;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.zeabay.common.s3.ZeabayS3Properties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Auto-configures AWS SDK v2 clients for S3-compatible object storage (e.g. MinIO).
 *
 * <p>Provides {@link S3AsyncClient} for async operations and {@link S3Presigner} for presigned URL
 * generation. Both are configured for path-style access and point to the endpoint defined in {@link
 * ZeabayS3Properties}.
 *
 * <p>Activates when the S3 SDK is on the classpath. Services that need S3 just add {@code
 * zeabay-s3} as a dependency — no manual bean configuration required.
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@ConditionalOnClass(S3AsyncClient.class)
@EnableConfigurationProperties(ZeabayS3Properties.class)
public class ZeabayS3AutoConfiguration {

  private final ZeabayS3Properties properties;

  /** Creates an async S3 client with path-style access for MinIO compatibility. */
  @Bean
  @ConditionalOnMissingBean
  public S3AsyncClient zeabayS3AsyncClient() {
    log.info("[zeabay-s3] Configuring S3AsyncClient for endpoint: {}", properties.getEndpoint());
    return S3AsyncClient.builder()
        .endpointOverride(URI.create(properties.getEndpoint()))
        .credentialsProvider(credentialsProvider())
        .region(Region.US_EAST_1)
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

  /** Creates an S3 presigner for generating presigned upload/download URLs. */
  @Bean
  @ConditionalOnMissingBean
  public S3Presigner zeabayS3Presigner() {
    return S3Presigner.builder()
        .endpointOverride(URI.create(properties.getEndpoint()))
        .credentialsProvider(credentialsProvider())
        .region(Region.US_EAST_1)
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

  private StaticCredentialsProvider credentialsProvider() {
    return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
  }
}
