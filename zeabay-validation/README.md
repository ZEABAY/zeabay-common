# Zeabay Validation

The `zeabay-validation` module provides a centralized approach to input validation, ensuring that all Zeabay microservices enforce the same rules and return consistent error formats.

## 🛠️ Technology Stack
- **Jakarta Bean Validation (JSR-303/JSR-380)**: Standard validation API.
- **Hibernate Validator**: The reference implementation.

## 📦 Core Components

### 1. `ZeabayValidator`
A wrapper around the Jakarta `Validator` that simplifies programmatic validation within services.

### 2. `ValidationError`
A standard model used by `zeabay-webflux` to represent validation failures in API responses.

## 🚀 How to Use

### 1. Declarative Validation (Controllers)
Annotate your DTOs with standard JSR-303 annotations:

```java
public record UserRegistrationRequest(
    @NotBlank String username,
    @Email @NotBlank String email,
    @Size(min = 8) String password
) {}

@PostMapping
public Mono<Void> register(@Valid @RequestBody UserRegistrationRequest request) {
    // ...
}
```

### 2. Programmatic Validation (Services)
Inject `ZeabayValidator` for manual validation:

```java
public void process(MyDto dto) {
    validator.validate(dto); // Throws exception if invalid
}
```

## ⚠️ System Impact
- **Consistency**: API users receive the same error structure regardless of which service they call.
- **Security**: Prevents malformed data from reaching the core business logic.
- **UX**: Provides detailed feedback (which field failed and why) to the frontend.
