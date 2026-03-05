# Zeabay Logging

The `zeabay-logging` module provides a non-invasive, aspect-oriented way to track method execution and propagate trace context throughout the reactive pipeline.

## 🛠️ Technology Stack
- **AspectJ**: For runtime method interception.
- **SLF4J / Logback**: Logging foundation.
- **MDC (Mapped Diagnostic Context)**: Context propagation.

## 📦 Core Components

### 1. `@Loggable` Annotation
A custom annotation that can be applied to classes or specific methods to enable automated logging.

### 2. `LoggingAspect`
The core aspect that intercepts `@Loggable` methods.
- Logs **Arguments** (optionally hidden).
- Logs **Result/Return Value** (optionally hidden).
- Logs **Execution Duration** in milliseconds.
- Supports **Reactive Types** (`Mono`, `Flux`) by preserving context through the reactive flow.

## 🚀 How to Use

### 1. Annotate your class/method
```java
@Service
@Loggable // Logs all methods in this service
public class MyService {

    @Loggable(logArgs = false) // Override: hide arguments for this specific method
    public Mono<Void> processSensitiveData(String password) {
        return Mono.empty();
    }
}
```

### 2. Expected Output
Logs will follow this standard format including Trace ID and request metadata:
`[traceId=abc-123, ip=127.0.0.1, user=admin] [GET /users/1] MyService.process ==> called with args: [1]`
`[traceId=abc-123, ip=127.0.0.1, user=admin] [GET /users/1] MyService.process <== completed in 45ms`

## ⚙️ Configuration
Auto-configuration is active by default. You can disable it via standard Spring property overrides if needed.

## ⚠️ System Impact
- **Observability**: Dramatically simplifies debugging by providing a clear "breadcrumb" trail of request execution.
- **Performance**: While AOP has a minor overhead, it is negligible compared to the value of structured traces in a distributed system.
- **Security**: Be careful with `logArgs = true` on methods that handle PII or credentials.
