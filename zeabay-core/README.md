# Zeabay Core

The `zeabay-core` module provides the foundational building blocks for all Zeabay microservices. It contains core utilities, base exceptions, and system-wide constants.

## 🛠️ Technology Stack
- **Java 25**: Utilizes modern Java features.
- **TSID**: Time-Sorted Unique Identifiers for high-performance indexing.
- **Spring Boot Auto-configuration**: Automatic bean registration.

## 📦 Core Components

### 1. `BusinessException` & `ErrorCode`
Standardizes how business-level errors are handled and propagated.
- `BusinessException`: The base runtime exception for domain logic errors. Carries a typed `ErrorCode`.
- `ErrorCode`: Enum defining the canonical error code taxonomy shared across all services.

| Code | Typical HTTP Status |
|---|---|
| `VALIDATION_ERROR` | 400 |
| `BAD_REQUEST` | 400 |
| `NOT_FOUND` | 404 |
| `USER_ALREADY_EXISTS` | 409 |
| `UNAUTHORIZED` | 401 |
| `FORBIDDEN` | 403 |
| `BUSINESS_ERROR` | 400 |
| `INTERNAL_ERROR` | 500 |

### 2. `TsidIdGenerator`
A high-performance ID generator that produces time-sorted, unique IDs. IDs are monotonically increasing within the same millisecond, making them ideal for B-Tree indexes.

| Method | Return type | Description |
|---|---|---|
| `newLongId()` | `long` | 64-bit TSID for BIGINT primary keys. |
| `newId()` | `String` | 13-char lowercase Crockford Base32 string (e.g. `054kg95e3i5`). |

### 3. `ObjectMapper` (bean)
Jackson `ObjectMapper` with `JavaTimeModule` and ISO-8601 date format (`WRITE_DATES_AS_TIMESTAMPS` disabled). Used by outbox event serialization, Kafka, and REST API. Registered when no other `ObjectMapper` bean exists.

### 4. `ZeabayConstants`
Centralized repository for Reactor context keys and HTTP header names used in distributed tracing.

| Constant | Value |
|---|---|
| `TRACE_ID_CTX_KEY` | `"traceId"` |
| `TRACE_ID_HEADER` | `"X-Trace-Id"` |
| `IP_CTX_KEY` | `"ip"` |
| `USER_CTX_KEY` | `"user"` |

## 🚀 How to Use

### Throwing a business exception
```java
if (user == null) {
    return Mono.error(new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
}
```

### Generating a TSID
Inject `TsidIdGenerator` as a Spring bean (registered by `ZeabayCommonAutoConfiguration`):

```java
@Service
@RequiredArgsConstructor
public class MyService {
    private final TsidIdGenerator idGenerator;

    public void createEntity() {
        long id = idGenerator.newLongId();
        // ...
    }
}
```

## ⚙️ Configuration

Enabled automatically via `ZeabayCommonAutoConfiguration`. No additional configuration required.

## ⚠️ System Impact
- **Standardization**: Forces a consistent error handling pattern across all services.
- **Database Performance**: Using TSID instead of UUID or random longs significantly improves insert performance and reduces B-Tree index fragmentation.
