package com.zeabay.common.kafka.event.user;

import com.zeabay.common.kafka.BaseEvent;
import java.time.Instant;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class UserRegisteredEvent extends BaseEvent {

  public static final String EVENT_TYPE = "UserRegisteredEvent";

  private final String userId;
  private final String email;
  private final String username;

  @Builder
  public UserRegisteredEvent(
      String eventId,
      String traceId,
      Instant occurredAt,
      String userId,
      String email,
      String username) {
    super(eventId, traceId, occurredAt);
    this.userId = userId;
    this.email = email;
    this.username = username;
  }

  @Override
  public String getEventType() {
    return EVENT_TYPE;
  }
}
