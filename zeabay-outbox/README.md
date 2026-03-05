# Zeabay Outbox

The `zeabay-outbox` module implements the **Transactional Outbox Pattern**, ensuring that domain events are published to Kafka *reliably* and only when business database transactions succeed.

## 🛠️ Technology Stack
- **Spring Data R2DBC**: For storing events in the same transaction as business data.
- **Spring Kafka**: For publishing events.
- **Spring Scheduling**: For background polling.

## 📦 Core Components

### 1. `OutboxEvent`
The entity stored in the database representing an event waiting to be published.

### 2. `OutboxPublisher`
A background worker that polls the `outbox_events` table at regular intervals, publishes PENDING messages to Kafka, and marks them as PUBLISHED (or FAILED after retries).

### 3. `OutboxProperties`
Configuration for polling intervals, batch sizes, and retry limits.

## 🚀 How to Use

### 1. Database Setup
Ensure you have an `outbox_events` table in your service's database.

### 2. Save Event in Transaction
Instead of publishing directly to Kafka, save the event to the outbox repository within your service's business logic:

```java
@Transactional
public Mono<Void> registerUser(User user) {
    return userRepository.save(user)
        .flatMap(u -> outboxRepository.save(new OutboxEvent(
            "user-topic", 
            u.getId(), 
            "USER_REGISTERED", 
            serialize(u))));
}
```

The `OutboxPublisher` will then automatically pick up this event and send it to Kafka.

## ⚙️ Configuration (application.yml)
```yaml
zeabay:
  outbox:
    polling-interval-ms: 1000
    batch-size: 50
    max-retries: 5
```

## ⚠️ System Impact
- **Distributed Consistency**: Solves the problem of "dual writes" (DB and Kafka). Guarantees **At-Least-Once** delivery.
- **Performance**: Polling introduces a slight delay (specified by the interval), but ensures the main user transaction is not blocked by Kafka availability.
- **Infra Simplicity**: Uses database polling instead of complex Change Data Capture (CDC) tools like Debezium.
