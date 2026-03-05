# Zeabay WebFlux

The `zeabay-webflux` module adds reactive web capabilities and cross-cutting web concerns to Zeabay microservices. It standardizes API responses, error handling, and request-level observability.

## 🛠️ Technology Stack
- **Spring WebFlux**: Reactive web framework.
- **Project Reactor**: The underlying reactive library (Mono/Flux).
- **MDC (Mapped Diagnostic Context)**: For distributed tracing.

## 📦 Core Components

### 1. `ZeabayGlobalExceptionHandler`
A `@RestControllerAdvice` that catches all exceptions and transforms them into a standard `ZeabayApiResponse` format:

| Exception | HTTP Status |
|---|---|
| `WebExchangeBindException` (validation) | 400 with per-field errors |
| `BusinessException(NOT_FOUND)` | 404 |
| `BusinessException(USER_ALREADY_EXISTS)` | 409 |
| `BusinessException(UNAUTHORIZED)` | 401 |
| `BusinessException(FORBIDDEN)` | 403 |
| `BusinessException(INTERNAL_ERROR)` | 500 |
| Any other `BusinessException` | 400 |
| `Throwable` (catch-all) | 500 |

### 2. `ZeabayTraceIdAutoConfiguration`
- Parses the incoming `traceparent` (W3C) or `X-Trace-Id` header, or generates a UUID if absent.
- Writes the trace ID to the `X-Trace-Id` response header and the Reactor context.
- Installs a Reactor `onEachOperator` hook that bridges the Reactor context to MDC so all log statements include the trace ID.
- Registers an `ExchangeFilterFunction` on `WebClient` to propagate the trace ID on outbound calls.

### 3. `ZeabayApiResponse<T>`
Universal top-level response envelope:
```json
{
  "success": true,
  "data": { "id": "817408902493679116", "username": "zeyneltest" },
  "error": null,
  "traceId": "4c015ac129fc4fc3950a6e0d699389b2",
  "timestamp": "2026-03-05T14:51:09.118092Z"
}
```

Error envelope:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "USER_ALREADY_EXISTS",
    "message": "Email is already registered",
    "path": "/api/v1/auth/register",
    "timestamp": "2026-03-05T14:51:09.118092Z",
    "validationErrors": null
  },
  "traceId": "4c015ac129fc4fc3950a6e0d699389b2",
  "timestamp": "2026-03-05T14:51:09.118092Z"
}
```

### 4. `ZeabayResponses`
Static factory utilities used in controllers:

```java
@PostMapping("/register")
public Mono<ResponseEntity<ZeabayApiResponse<RegisterApiResponse>>> register(
    @Valid @RequestBody RegisterRequest request) {
    return authService.registerUser(mapper.toCommand(request))
        .map(mapper::toApiResponse)
        .flatMap(data -> ZeabayResponses.created(data, URI.create("/api/v1/users/" + data.id())));
}
```

## 🚀 Auto-configured Beans

| Bean / Auto-configuration | Purpose |
|---|---|
| `ZeabayGlobalExceptionHandler` | Converts exceptions → `ZeabayApiResponse` |
| `traceIdWebFilter` | Inbound trace ID parsing and response header |
| `zeabayRequestContextWebFilter` | Writes IP, user, method, path to Reactor context |
| `reactorMdcTraceIdHook` | Bridges Reactor context → MDC for every operator |
| `zeabayTraceIdWebClientFilter` | Outbound trace ID propagation on `WebClient` |
| `corsWebFilter` | Dev-mode permissive CORS (exposes `X-Trace-Id`, `traceparent`) |

## ⚠️ System Impact
- **Observability**: Every log entry for a web request contains a `traceId`, enabling end-to-end trace reconstruction in Jaeger/Grafana.
- **Consistency**: Frontend teams rely on a single response structure across all services.
- **Reliability**: Prevents raw stack traces from leaking to clients during failures.
