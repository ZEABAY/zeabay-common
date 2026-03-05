# Zeabay Core

The `zeabay-core` module provides the foundational building blocks for all Zeabay microservices. it contains core utilities, base exceptions, and system-wide constants.

## 🛠️ Technology Stack
- **Java 25**: Utilizes modern Java features.
- **TSID**: Time-Sorted Unique Identifiers for high-performance indexing.
- **Spring Boot Auto-configuration**: Automatic bean registration.

## 📦 Core Components

### 1. `BusinessException` & `ErrorCode`
Standardizes how business-level errors are handled and propagated.
- `BusinessException`: The base runtime exception for domain logic errors.
- `ErrorCode`: An interface/enum defining standard error codes.

### 2. `TsidIdGenerator`
A high-performance ID generator that produces 64-bit long IDs which are time-sorted, making them extremely database-friendly (especially for B-Tree indexes).

### 3. `ZeabayConstants`
Centralized repository for constants used across multiple modules (e.g., Trace ID keys, MDC headers).

## 🚀 How to Use

### Global Exception Handling
In your services, throw `BusinessException` for expected domain failures:

```java
if (userNotFound) {
    throw new BusinessException(ErrorCode.USER_NOT_FOUND, "User ID not found in database");
}
```

### ID Generation
Inject the `TsidIdGenerator` to generate unique, sortable IDs:

```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final TsidIdGenerator idGenerator;

    public void createEntity() {
        long id = idGenerator.generate();
        // ...
    }
}
```

## ⚙️ Configuration

Enabled automatically via `ZeabayCommonAutoConfiguration`.

## ⚠️ System Impact
- **Standardization**: Forces a consistent error handling pattern across all services.
- **Database Performance**: Using TSID instead of UUID or random longs improves insert performance and index fragmentation in RDBMS/R2DBC.
