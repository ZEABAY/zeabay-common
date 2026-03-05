# Zeabay Security

The `zeabay-security` module provides a hardened, reactive security foundation for Zeabay microservices, integrating with Keycloak as the OAuth2/OIDC Identity Provider.

## 🛠️ Technology Stack
- **Spring Security (Reactive)**: Security framework for WebFlux.
- **OAuth2 Resource Server / JWT**: Stateless authentication via Keycloak-issued JWTs.
- **Spring Data R2DBC Auditing**: Integration with `createdBy` / `updatedBy` audit fields.

## 📦 Core Components

### 1. `ZeabaySecurityAutoConfiguration`
Registers a default `SecurityWebFilterChain` (`@ConditionalOnMissingBean`) that:
- Permits `/actuator/**` unauthenticated.
- Requires authentication on all other endpoints.
- Disables CSRF, HTTP Basic, and form login.
- Configures CORS from `ZeabaySecurityProperties`.

Services override the filter chain by declaring their own `SecurityWebFilterChain` bean (e.g. `auth-service` permits `/api/v1/auth/**` publicly).

### 2. `ZeabayReactiveAuditorAware`
Reads `Authentication.getName()` from `ReactiveSecurityContextHolder`; defaults to `"system"` when unauthenticated. Overrides the `zeabay-r2dbc` fallback bean so `createdBy` / `updatedBy` reflect the actual user.

### 3. `ZeabaySecurityProperties`
Typed YAML configuration for CORS and JWT token lifetimes.

| Property | Default |
|---|---|
| `zeabay.security.cors.allowed-origins` | `["http://localhost:3000"]` |
| `zeabay.security.cors.allowed-methods` | `[GET, POST, PUT, DELETE, OPTIONS, PATCH]` |
| `zeabay.security.cors.allowed-headers` | `["*"]` |
| `zeabay.security.cors.exposed-headers` | `["X-Trace-Id", "Authorization"]` |
| `zeabay.security.cors.allow-credentials` | `true` |
| `zeabay.security.jwt.access-token-expiry` | `15m` |
| `zeabay.security.jwt.refresh-token-expiry` | `7d` |

## 🚀 How to Use

### 1. JWT Resource Server Configuration
Configure the Keycloak issuer URI via standard Spring Security properties:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9080/realms/pulse
```

### 2. Role-based Access Control
Use `@PreAuthorize` for fine-grained RBAC once JWT validation is active:

```java
@GetMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public Mono<String> adminOnly() {
    return Mono.just("Hello Admin");
}
```

### 3. Custom Security Filter Chain
Override the default filter chain in your service:

```java
@Bean
public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
    return http
        .authorizeExchange(ex -> ex
            .pathMatchers("/api/v1/auth/**").permitAll()
            .anyExchange().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .build();
}
```

## ⚠️ System Impact
- **Security**: Enforces a "secure by default" posture — all endpoints require a valid JWT unless explicitly permitted.
- **Integration**: Zero-config CORS and auditor integration; only the Keycloak issuer URI needs to be set.
- **Observability**: Data changes are automatically attributed to the authenticated user via reactive auditing.
