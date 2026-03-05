# Zeabay Validation

The `zeabay-validation` module provides a centralized approach to input validation, ensuring that all Zeabay microservices enforce the same rules and return consistent error formats.

## 🛠️ Technology Stack
- **Jakarta Bean Validation (JSR-303/JSR-380)**: Standard validation API.
- **Hibernate Validator**: The reference implementation.

## 📦 Core Components

### 1. `ZeabayValidator`
A static utility class wrapping the Jakarta `Validator`. Used for programmatic validation inside services or Kafka consumers where Spring MVC binding is not available.

| Method | Description |
|---|---|
| `validate(T object)` | Runs Bean Validation; returns `List<ValidationError>` (empty if valid). |
| `isValid(T object)` | Convenience boolean check. |

### 2. `ValidationError`
A `record(String field, String message)` representing a single field-level violation.

The `zeabay-webflux` module also defines `com.zeabay.common.api.model.ValidationError` (identical shape) used inside `ErrorResponse` for HTTP API responses.

## 🚀 How to Use

### 1. Declarative Validation (Controllers)
Annotate your DTOs with standard JSR-303 annotations and use `@Valid` in the controller:

```java
public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 50) String username,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password
) {}

@PostMapping("/register")
public Mono<ResponseEntity<ZeabayApiResponse<RegisterApiResponse>>> register(
    @Valid @RequestBody RegisterRequest request) {
    // ...
}
```

`ZeabayGlobalExceptionHandler` catches `WebExchangeBindException` and returns a structured `400` with per-field errors automatically.

### 2. Programmatic Validation (Services)
Use the static methods directly — no injection needed:

```java
public Mono<Void> processEvent(MyDto dto) {
    List<ValidationError> errors = ZeabayValidator.validate(dto);
    if (!errors.isEmpty()) {
        return Mono.error(new BusinessException(ErrorCode.VALIDATION_ERROR,
            errors.get(0).message()));
    }
    return doProcess(dto);
}
```

## ⚠️ System Impact
- **Consistency**: API consumers receive the same error structure regardless of which service they call.
- **Security**: Prevents malformed data from reaching core business logic.
- **UX**: Provides detailed feedback (which field failed and why) to frontend clients.
