# Zeabay Ops

The `zeabay-ops` module provides production-ready operational defaults for Zeabay microservices, specifically tailoring Spring Boot Actuator settings for cloud-native (Kubernetes) environments.

## 🛠️ Technology Stack
- **Spring Boot Actuator**: Management and monitoring framework.
- **Micrometer**: Prometheus metrics support.

## 📦 Core Components

### 1. `ZeabayOpsDefaultsEnvironmentPostProcessor`
A high-precedence post-processor that injects default properties into the Spring Environment *before* the application starts. 

It automatically:
- Enables health probes (`liveness`, `readiness`).
- Shows detailed health information (conditionally).
- Enables Prometheus metrics endpoint.
- Exposes specific endpoints based on the active profile (`dev` vs `prod`).

## 🚀 How to Use

Enabled automatically when included. No code changes are required.

```xml
<dependency>
    <groupId>com.zeabay</groupId>
    <artifactId>zeabay-ops</artifactId>
</dependency>
```

## ⚙️ Behavior by Profile

### `dev` (Default)
Exposes: `health`, `info`, `metrics`, `prometheus`.

### `prod`
Exposes: `health` (minimal footprint).

## ⚠️ System Impact
- **Reliability**: Ensures that Kubernetes can correctly monitor service health without manual configuration.
- **Observability**: Standardizes metrics collection for Prometheus/Grafana dashboards across the entire ecosystem.
- **Security**: Reduces the attack surface by hiding sensitive management endpoints in production by default.
