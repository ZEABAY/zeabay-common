package com.zeabay.common.kafka;

/** Central registry of Kafka topic names used across Pulse services. */
public final class KafkaTopic {

  private KafkaTopic() {}

  public static final String USER_REGISTERED = "pulse.auth.user-registered";
  public static final String EMAIL_VERIFICATION = "pulse.auth.email-verification";
}
