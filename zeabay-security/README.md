# Zeabay Security

The `zeabay-security` module provides a hardened, reactive security foundation for Zeabay microservices, integrating seamlessly with IAM providers like Keycloak.

## 🛠️ Technology Stack
- **Spring Security (Reactive)**: Security framework for WebFlux.
- **OAuth2 / JWT**: Standard for stateless authentication.
- **Keycloak**: Primary Identity Provider (IDP).

## 📦 Core Components

### 1. `ZeabaySecurityAutoConfiguration`
Automatically secures all endpoints while providing sane defaults (like permitting health checks and Swagger UI). It configures the JWT decoder and resource server settings.

### 2. `ZeabayReactiveAuditorAware`
Integrates with the Security Context to provide the current user's ID to `zeabay-r2dbc` for auditing purposes (`createdBy`, `lastModifiedBy`).

### 3. `ZeabaySecurityProperties`
Exposes configuration properties to customize security behavior via YAML.

## 🚀 How to Use

### 1. Secure your Endpoints
By default, all endpoints are secured. Use `@PreAuthorize` for fine-grained role-based access control (RBAC):

```java
@GetMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public Mono<String> adminOnly() {
    return Mono.just("Hello Admin");
}
```

### 2. Configuration (application.yml)
```yaml
zeabay:
  security:
    enabled: true
    issuer-uri: https://keycloak.example.com/realms/zeabay
    resource: pulse-service
```

## ⚠️ System Impact
- **Security**: Ensures a "secure by default" posture across all microservices.
- **Integration**: Zero-config connection to Keycloak for authentication.
- **Observability**: Automatically attributes data changes to the specific user making the request.
