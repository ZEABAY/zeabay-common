# Zeabay Kafka

The `zeabay-kafka` module provides a standardized, reactive-friendly foundation for event-driven messaging across Zeabay microservices. It simplifies producer/consumer configuration and enforces best practices like DLQs and idempotence.

## 🛠️ Technology Stack
- **Spring Kafka**: Spring context for Apache Kafka.
- **Apache Kafka**: Distributed event streaming platform.
- **Project Reactor**: For reactive event processing.

## 📦 Core Components

### 1. `BaseEvent`
A baseline class for all Kafka event payloads. It includes metadata like `aggregateId`, `eventType`, `traceId`, and `timestamp`.

### 2. `ZeabayKafkaProperties`
Typed configuration for Kafka settings, including producer/consumer tuning and DLQ (Dead Letter Queue) parameters.

### 3. `ZeabayKafkaAutoConfiguration`
Automatically configures `KafkaTemplate`, `ConsumerFactory`, and `ProducerFactory` with Zeabay defaults.

## 🚀 How to Use

### 1. Define an Event
Extend `BaseEvent` for your message payloads:

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class UserRegisteredEvent extends BaseEvent {
    private String username;
    private String email;
}
```

### 2. Publish an Event
```java
@Service
@RequiredArgsConstructor
public class MyProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(UserRegisteredEvent event) {
        kafkaTemplate.send("user-topic", String.valueOf(event.getAggregateId()), event);
    }
}
```

## ⚙️ Configuration (application.yml)
```yaml
zeabay:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      acks: all
      enable-idempotence: true
    dlq:
      enabled: true
      suffix: ".dlq"
```

## ⚠️ System Impact
- **Reliability**: Idempotent producers and DLQs prevent data loss and duplicates during transient failures.
- **Scalability**: Decouples microservices through asynchronous, event-driven communication.
- **Observability**: Trace IDs are propagated through event headers, allowing end-to-end flow tracking across service boundaries.
