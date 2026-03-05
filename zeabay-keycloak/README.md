# Zeabay Keycloak

The `zeabay-keycloak` module provides a simplified API and client for interacting with Keycloak Identity Provider (IDP). It encapsulates administrative tasks like user registration and token operations.

## 🛠️ Technology Stack
- **Keycloak Admin Client**: Java SDK for Keycloak management.
- **WebClient (Spring Framework)**: For high-performance reactive HTTP calls.
- **Project Reactor**: Asynchronous execution.

## 📦 Core Components

### 1. `ZeabayKeycloakClient`
The primary interface for microservices. It provides methods for:
- `registerUser`: Creates a new user in Keycloak with a password.
- `loginUser`: Exchanges credentials for JWT access/refresh tokens.

### 2. `KeycloakProperties`
Configuration class for realm names, client IDs, secrets, and auth server URLs.

## 🚀 How to Use

### 1. Configuration
Set up your Keycloak details in `application.yml`:
```yaml
zeabay:
  keycloak:
    auth-server-url: https://idp.zeabay.com
    realm: zeabay
    resource: pulse-client
    credentials:
      secret: ${KEYCLOAK_SECRET}
```

### 2. Use the Client
Inject `ZeabayKeycloakClient` into your registration or auth service:

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    private final ZeabayKeycloakClient keycloakClient;

    public Mono<String> signup(RegistrationRequest req) {
        return keycloakClient.registerUser(new KeycloakRegistrationRequest(
            req.username(), req.email(), req.password()));
    }
}
```

## ⚠️ System Impact
- **Decoupling**: Business services don't need to know the internal details of Keycloak's APIs or SDKs.
- **Reliability**: Uses `Schedulers.boundedElastic()` for blocking Admin SDK calls to ensure they don't starve the Netty event loop.
- **Security**: Centralizes sensitive operations like credential handling in a single, tested library.
