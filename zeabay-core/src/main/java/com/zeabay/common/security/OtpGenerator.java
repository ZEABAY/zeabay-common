package com.zeabay.common.security;

import java.security.SecureRandom;

/** Generates cryptographically secure 6-digit OTPs. */
public class OtpGenerator {

  private static final int MIN = 100_000;
  private static final int RANGE = 900_000;

  private final SecureRandom random;

  public OtpGenerator() {
    this.random = new SecureRandom();
  }

  public String generate() {
    return String.valueOf(MIN + random.nextInt(RANGE));
  }
}
