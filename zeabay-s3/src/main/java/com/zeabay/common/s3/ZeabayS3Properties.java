package com.zeabay.common.s3;

import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration properties for S3-compatible object storage (e.g. MinIO, AWS S3).
 *
 * <p>Binds to the {@code zeabay.s3} prefix in application configuration. Each service can customize
 * its own bucket name, file size limits, and presigned URL expiry.
 *
 * <p>Uses explicit {@link Getter}/{@link Setter} instead of {@code @Data} to prevent credential
 * leakage via {@link #toString()}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "zeabay.s3")
public class ZeabayS3Properties {

  /** S3 endpoint URL (e.g. {@code http://localhost:9000} for MinIO). */
  private String endpoint;

  /** Access key for S3 authentication. */
  private String accessKey;

  /** Secret key for S3 authentication. */
  private String secretKey;

  /** Default bucket name. */
  private String bucket;

  /** Presigned URL expiry duration in seconds. Default: 5 minutes. */
  private int presignedUrlExpirySeconds = 300;

  /** Maximum allowed upload file size in bytes. Default: 5 MB. */
  private long maxFileSize = 5_242_880L;

  /** Allowed MIME types for file uploads. Default: JPEG, PNG, WebP. */
  private Set<String> allowedContentTypes = Set.of("image/jpeg", "image/png", "image/webp");

  /** Excludes credentials — only logs endpoint and bucket. */
  @Override
  public String toString() {
    return "ZeabayS3Properties{endpoint='%s', bucket='%s'}".formatted(endpoint, bucket);
  }
}
