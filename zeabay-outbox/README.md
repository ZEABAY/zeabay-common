# Zeabay Outbox

The `zeabay-outbox` module implements the **Transactional Outbox Pattern**, ensuring that domain events are published to Kafka *reliably* and only when the business database transaction succeeds.

## 🛠️ Technology Stack
- **Spring Data R2DBC**: For storing events in the same transaction as business data.
- **Spring Kafka**: For publishing events.
- **Spring Scheduling**: For background polling.

## 📦 Core Components

### 1. `OutboxEvent`
R2DBC entity mapped to `outbox_events` table.

| Field | Type | Description |
|---|---|---|
| `id` | `Long` | TSID primary key, assigned via `@Builder.Default`. |
| `eventType` | `String` | Logical event name (e.g. `"UserRegisteredEvent"`). |
| `topic` | `String` | Kafka topic to publish to. |
| `aggregateType` | `String` | Domain aggregate (e.g. `"User"`). |
| `aggregateId` | `Long` | TSID of the aggregate root. |
| `payload` | `String` | JSON-serialized event body. |
| `traceId` | `String` | Propagated trace ID for distributed tracing. |
| `status` | `Status` | `PENDING` → `PUBLISHED` or `FAILED`. |
| `retryCount` | `int` | Incremented on each failed publish attempt. |

### 2. `OutboxPublisher`
Background scheduler that polls `outbox_events` and drains PENDING events to Kafka:
- Marks events `PUBLISHED` on success.
- Increments `retryCount` on failure; marks `FAILED` once `maxRetries` is exceeded.

### 3. `OutboxProperties`
| Property | Default | Description |
|---|---|---|
| `zeabay.outbox.polling-interval` | `1s` | How often to poll for pending events. |
| `zeabay.outbox.batch-size` | `50` | Max events per polling cycle. |
| `zeabay.outbox.max-retries` | `3` | Max retry attempts before marking FAILED. |

## 🚀 How to Use

### 1. Database Setup
The `outbox_events` table is created automatically by `ZeabayOutboxAutoConfiguration` via the bundled `schema-outbox.sql` (`CREATE TABLE IF NOT EXISTS`). No Flyway migration needed.

### 2. Save Event in Transaction
Instead of publishing directly to Kafka, save the event within your business transaction using the builder:

```java
@Transactional
public Mono<AuthUser> registerUser(RegisterUserCommand cmd) {
    return userRepository.save(buildUser(cmd))
        .flatMap(user -> {
            var event = OutboxEvent.builder()
                .eventType(UserRegisteredEvent.EVENT_TYPE)
                .topic("pulse.user.registered")
                .aggregateType("User")
                .aggregateId(user.getId())
                .payload(objectMapper.writeValueAsString(toEvent(user)))
                .traceId(currentTraceId())
                .build();
            return outboxEventRepository.save(event).thenReturn(user);
        });
}
```

The `OutboxPublisher` will automatically pick up this event and publish it to Kafka.

## ⚙️ Configuration (application.yml)
```yaml
zeabay:
  outbox:
    polling-interval: 1s
    batch-size: 50
    max-retries: 3
```

## ⚠️ System Impact
- **Distributed Consistency**: Solves the "dual write" problem (DB + Kafka). Guarantees **at-least-once** delivery.
- **Performance**: Polling introduces a slight delay (configured by `polling-interval`), but the main business transaction is never blocked by Kafka availability.
- **Infra Simplicity**: Uses database polling instead of complex Change Data Capture (CDC) tools like Debezium.
