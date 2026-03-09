package com.zeabay.common.security;

import java.security.SecureRandom;

/**
 * Generates cryptographically random one-time passwords (OTPs) for email verification flows.
 *
 * <p>Registered as a Spring bean via {@link
 * com.zeabay.common.autoconfigure.ZeabayCommonAutoConfiguration}. Inject this bean rather than
 * instantiating it directly to guarantee a single {@link SecureRandom} instance across the
 * application (SecureRandom initialization is expensive).
 */
public class OtpGenerator {

  private static final int MIN = 100_000;
  private static final int RANGE = 900_000; // yields 100000..999999

  private final SecureRandom random;

  public OtpGenerator() {
    this.random = new SecureRandom();
  }

  /**
   * Returns a cryptographically random 6-digit numeric OTP as a {@code String}, e.g. {@code
   * "473821"}.
   */
  public String generate() {
    return String.valueOf(MIN + random.nextInt(RANGE));
  }
}
