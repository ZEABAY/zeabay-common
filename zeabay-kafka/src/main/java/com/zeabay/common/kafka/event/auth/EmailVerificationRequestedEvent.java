package com.zeabay.common.kafka.event.auth;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.zeabay.common.kafka.BaseEvent;
import java.time.Instant;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonDeserialize(builder = EmailVerificationRequestedEvent.EmailVerificationRequestedEventBuilder.class)
public class EmailVerificationRequestedEvent extends BaseEvent {

  public static final String EVENT_TYPE = "EmailVerificationRequested";

  private final String userId;
  private final String email;
  private final String verificationToken;

  @Builder
  public EmailVerificationRequestedEvent(
      @JsonProperty("eventId") @JsonAlias("event_id") String eventId,
      @JsonProperty("traceId") @JsonAlias("trace_id") String traceId,
      @JsonProperty("occurredAt") @JsonAlias("occurred_at") Instant occurredAt,
      @JsonProperty("userId") @JsonAlias("user_id") String userId,
      @JsonProperty("email") String email,
      @JsonProperty("verificationToken") @JsonAlias("verification_token") String verificationToken) {
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
