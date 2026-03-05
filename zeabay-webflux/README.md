# Zeabay WebFlux

The `zeabay-webflux` module adds reactive web capabilites and cross-cutting web concerns to Zeabay microservices. It standardizes API responses, error handling, and request-level observability.

## 🛠️ Technology Stack
- **Spring WebFlux**: Reactive web framework.
- **Project Reactor**: The underlying reactive library (Mono/Flux).
- **MDC (Mapped Diagnostic Context)**: For distributed tracing.

## 📦 Core Components

### 1. `ZeabayGlobalExceptionHandler`
A `@RestControllerAdvice` that catches all exceptions (including `BusinessException`) and transforms them into a standard `ErrorResponse` format.

### 2. `ZeabayRequestContextWebFilter`
A high-precedence filter that extracts/generates a **Trace ID** for every incoming request and puts it into the reactive context.

### 3. `ZeabayApiResponse`
A standard wrapper for all API responses to ensure consistency across the mobile app and frontend.

## 🚀 How to Use

### 1. Unified Responses
Use `ZeabayResponses` utility to return consistent data:

```java
@GetMapping("/{id}")
public Mono<ZeabayApiResponse<UserDto>> getUser(@PathVariable Long id) {
    return service.findById(id)
            .map(ZeabayResponses::ok);
}
```

### 2. Error Handling
Errors are automatically handled. If a `BusinessException` is thrown, the client receives:
```json
{
  "timestamp": "2024-03-03T10:00:00Z",
  "status": 400,
  "errorCode": "USER_NOT_FOUND",
  "message": "User ID not found in database",
  "traceId": "abc-123-xyz"
}
```

## ⚙️ Configuration (application.yml)
Auto-configuration is active by default. You can customize CORS via:
```yaml
zeabay:
  web:
    cors:
      allowed-origins: "*"
      allowed-methods: "GET,POST,PUT,DELETE"
```

## ⚠️ System Impact
- **Observability**: Ensures every log entry associated with a web request contains a `traceId`.
- **Consistency**: Frontend teams can rely on a single response structure for all services.
- **Reliability**: Prevents raw stack traces from leaking to clients during failures.
