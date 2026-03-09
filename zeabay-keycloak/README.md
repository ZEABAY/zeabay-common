# Zeabay Keycloak

The `zeabay-keycloak` module provides a reactive façade over the Keycloak Identity Provider (IDP). It encapsulates user registration, token operations, and administrative actions behind a clean, injectable client.

## 🛠️ Technology Stack
- **Keycloak Admin Client**: Java SDK for Keycloak management (blocking, offloaded to `boundedElastic`).
- **WebClient (Spring Framework)**: For reactive HTTP calls to the OIDC token endpoint.
- **Project Reactor**: Asynchronous execution.

## 📦 Core Components

### 1. `ZeabayKeycloakClient`
The primary interface for microservices.

| Method | Description |
|---|---|
| `registerUser(KeycloakRegistrationRequest)` | Creates a new user in Keycloak; returns the Keycloak user UUID. |
| `loginUser(KeycloakTokenRequest)` | Exchanges credentials for JWT access/refresh tokens (`password` grant). |
| `refreshToken(String refreshToken)` | Exchanges a refresh token for a new token pair (`refresh_token` grant). |
| `setEmailVerified(String keycloakId, boolean verified)` | Updates the `emailVerified` flag on a Keycloak user. |
| `logout(String keycloakId)` | Invalidates all sessions for the user via Admin SDK. |

### 2. `KeycloakProperties`
Configuration record bound to the `keycloak.*` YAML prefix.

### 3. DTOs
| Class | Purpose |
|---|---|
| `KeycloakRegistrationRequest` | Input for `registerUser` (`username`, `email`, `password`). |
| `KeycloakTokenRequest` | Input for `loginUser` (`usernameOrEmail`, `password`). |
| `ZeabayTokenResponse` | Output token pair (`accessToken`, `refreshToken`, `expiresIn`). |

## 🚀 How to Use

### 1. Configuration (`application.yml`)
```yaml
keycloak:
  auth-server-url: http://localhost:9080
  realm: pulse
  resource: pulse-client
  credentials:
    secret: ${KEYCLOAK_CLIENT_SECRET}
  admin:
    username: ${KEYCLOAK_ADMIN_USERNAME}
    password: ${KEYCLOAK_ADMIN_PASSWORD}
```

`ZeabayKeycloakAutoConfiguration` activates only when `keycloak.auth-server-url` is present.

### 2. Use the Client
Inject `ZeabayKeycloakClient` into your service:

```java
@Service
@RequiredArgsConstructor
public class AuthServiceImpl {
    private final ZeabayKeycloakClient keycloakClient;

    public Mono<String> signup(RegisterUserCommand cmd) {
        return keycloakClient.registerUser(
            KeycloakRegistrationRequest.builder()
                .username(cmd.username())
                .email(cmd.email())
                .password(cmd.password())
                .build());
    }
}
```

## ⚠️ System Impact
- **Decoupling**: Business services don't need to know Keycloak's internal APIs or SDK details.
- **Reliability**: Blocking Admin SDK calls are offloaded to `Schedulers.boundedElastic()` so they don't starve the Netty event loop.
- **Security**: Centralizes sensitive credential handling in a single, tested library.
