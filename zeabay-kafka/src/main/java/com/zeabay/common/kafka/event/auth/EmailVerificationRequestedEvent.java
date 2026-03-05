package com.zeabay.common.kafka.event.auth;

import com.zeabay.common.kafka.BaseEvent;
import java.time.Instant;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EmailVerificationRequestedEvent extends BaseEvent {

  public static final String EVENT_TYPE = "EmailVerificationRequested";

  private final String userId;
  private final String email;
  private final String verificationToken;

  @Builder
  public EmailVerificationRequestedEvent(
      String eventId,
      String traceId,
      Instant occurredAt,
      String userId,
      String email,
      String verificationToken) {
    super(eventId, traceId, occurredAt);
    this.userId = userId;
    this.email = email;
    this.verificationToken = verificationToken;
  }

  @Override
  public String getEventType() {
    return EVENT_TYPE;
  }
}
