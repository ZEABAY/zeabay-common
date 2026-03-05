# Zeabay Kafka

The `zeabay-kafka` module provides a standardized, reactive-friendly foundation for event-driven messaging across Zeabay microservices. It enforces a consistent event schema and simplifies producer/consumer configuration.

## 🛠️ Technology Stack
- **Spring Kafka**: Spring context for Apache Kafka.
- **Apache Kafka** (KRaft mode): Distributed event streaming platform.
- **Project Reactor**: For reactive event processing.

## 📦 Core Components

### 1. `BaseEvent`
Abstract baseline class for all Kafka event payloads. Every concrete event carries:

| Field | Type | Description |
|---|---|---|
| `eventId` | `String` | TSID string — unique event identifier for deduplication. |
| `traceId` | `String` | Distributed trace ID propagated from the originating request. |
| `occurredAt` | `Instant` | Wall-clock time of the event. |
| `getEventType()` | `abstract String` | Logical event name (e.g. `"UserRegisteredEvent"`). |

### 2. Pre-built Domain Events

| Class | Package | Topic | Consumer |
|---|---|---|---|
| `UserRegisteredEvent` | `kafka.event.user` | `pulse.user.registered` | user-profile-service |
| `EmailVerificationRequestedEvent` | `kafka.event.auth` | `pulse.auth.email-verification` | mail-service |

### 3. `ZeabayKafkaProperties`
Typed configuration with production-safe defaults:

| Property prefix | Key defaults |
|---|---|
| `zeabay.kafka.producer` | `acks=all`, `retries=3`, `enableIdempotence=true` |
| `zeabay.kafka.consumer` | `groupIdPrefix=pulse`, `autoOffsetReset=earliest`, `enableAutoCommit=false` |
| `zeabay.kafka.dlq` | `enabled=true`, `suffix=".dlq"`, `maxAttempts=3` |

### 4. `ZeabayKafkaAutoConfiguration`
Automatically configures `KafkaTemplate`, `ConsumerFactory`, and `ProducerFactory` using `ZeabayKafkaProperties`. All beans are `@ConditionalOnMissingBean` — override as needed.

## 🚀 How to Use

### 1. Define a Domain Event
Extend `BaseEvent` and provide the event type constant:

```java
@Getter
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "OrderCreatedEvent";

    private final String orderId;
    private final Long customerId;

    @Builder
    public OrderCreatedEvent(String eventId, String traceId, Instant occurredAt,
                             String orderId, Long customerId) {
        super(eventId, traceId, occurredAt);
        this.orderId = orderId;
        this.customerId = customerId;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
}
```

### 2. Publish via Transactional Outbox
Always publish domain events through `zeabay-outbox` rather than calling `KafkaTemplate` directly. This guarantees at-least-once delivery even if Kafka is temporarily unavailable.

### 3. Configuration (application.yml)
```yaml
zeabay:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      acks: all
      enable-idempotence: true
    dlq:
      enabled: true
      suffix: .dlq
      max-attempts: 3
```

## ⚠️ System Impact
- **Reliability**: Idempotent producers and DLQs prevent data loss during transient failures.
- **Scalability**: Decouples microservices through asynchronous, event-driven communication.
- **Observability**: `traceId` is carried in every event, enabling end-to-end request tracing across service boundaries.
