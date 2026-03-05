# Zeabay Common Library

[![Java Version](https://img.shields.io/badge/Java-25-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk25-relnotes.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-brightgreen.svg)](https://spring.io/projects/spring-boot)

A robust, reactive common library designed for microservices architecture, providing essential building blocks, auto-configurations, and cross-cutting concerns for the Zeabay ecosystem.

## 🚀 Core Technologies

- **Java 25** (Latest LTS features)
- **Spring Boot 4.0.2**
- **Spring WebFlux** (Reactive stack)
- **Spring Data R2DBC**
- **Keycloak** (Security & IDP)
- **Apache Kafka** (Event-driven messaging)
- **MapStruct & Lombok** (Boilerplate reduction)

---

## 📦 Detailed Module Reference

### 🏗️ Foundation Modules

#### [`zeabay-bom`](./zeabay-bom/README.md)
- **Purpose**: Centralized dependency version management (Bill of Materials).
- **Technology**: Maven POM.
- **Usage**: Import in the `<dependencyManagement>` section of your project's root `pom.xml`.
- **Effect**: Ensures all Zeabay microservices use compatible versions of the library and third-party dependencies, simplifying upgrades.

#### [`zeabay-core`](./zeabay-core/README.md)
- **Purpose**: Provides essential utilities, common constants, and base exception classes.
- **Technology**: Java 25, TSID (Time-Sorted Unique Identifiers).
- **Usage**: Use `BusinessException` for domain errors and `TsidIdGenerator` for efficient ID generation.
- **Effect**: Standardizes error handling and ensures high-performance, sortable IDs across the system.

#### [`zeabay-ops`](./zeabay-ops/README.md)
- **Purpose**: Injects production-ready operational defaults.
- **Technology**: Spring Boot Actuator.
- **Usage**: Automatically active when included. Sets up health probes and Prometheus metrics.
- **Effect**: Eliminates manual configuration for Kubernetes liveness/readiness probes and monitoring endpoints.

---

### 🌐 Reactive Web & API

#### [`zeabay-webflux`](./zeabay-webflux/README.md)
- **Purpose**: Foundation for reactive web services.
- **Technology**: Spring WebFlux, Project Reactor.
- **Usage**: Auto-configures global error handlers and request context filters.
- **Effect**: Provides a unified `ZeabayApiResponse` structure and ensures Trace IDs are propagated through every reactive flow.

#### [`zeabay-openapi`](./zeabay-openapi/README.md)
- **Purpose**: Automated API documentation.
- **Technology**: `springdoc-openapi`.
- **Usage**: Configures Swagger UI at `/swagger-ui.html`.
- **Effect**: Keeps API documentation in sync with code without extra boilerplate.

#### [`zeabay-validation`](./zeabay-validation/README.md)
- **Purpose**: Centralized validation logic.
- **Technology**: Jakarta Bean Validation (Hibernate Validator).
- **Usage**: Use standard `@Valid` and JSR-303 annotations.
- **Effect**: Standardizes validation error responses across all microservices.

---

### 🔐 Identity & Security

#### [`zeabay-security`](./zeabay-security/README.md)
- **Purpose**: Reactive security configurations.
- **Technology**: Spring Security (WebFlux).
- **Usage**: Injected via auto-configuration to secure endpoints.
- **Effect**: Enforces consistent security policies and enables reactive auditing across the ecosystem.

#### [`zeabay-keycloak`](./zeabay-keycloak/README.md)
- **Purpose**: Simplified integration with Keycloak IDP.
- **Technology**: Keycloak Admin SDK, WebClient.
- **Usage**: Inject `ZeabayKeycloakClient` to perform user registration or fetch tokens.
- **Effect**: Moves complex authentication/registration logic into a reusable client.

#### [`zeabay-r2dbc`](./zeabay-r2dbc/README.md)
- **Purpose**: Reactive database metadata management.
- **Technology**: Spring Data R2DBC.
- **Usage**: Extend `BaseEntity` in your domain models.
- **Effect**: Automatically populates `created_at`, `updated_at`, and audit fields in a reactive manner.

---

### 📩 Event-Driven & Messaging

#### [`zeabay-kafka`](./zeabay-kafka/README.md)
- **Purpose**: Standardized Kafka messaging foundation.
- **Technology**: Spring Kafka, Reactor.
- **Usage**: Extend `BaseEvent` for your DTOs and use typed properties in YAML.
- **Effect**: Reliable messaging with built-in Dead Letter Queue (DLQ) support and idempotent producers.

#### [`zeabay-outbox`](./zeabay-outbox/README.md)
- **Purpose**: Implements the Outbox pattern for transactional integrity.
- **Technology**: R2DBC, Scheduled Polling, KafkaTemplate.
- **Usage**: Save events to an `outbox_events` table within your business transaction.
- **Effect**: Guarantees that external events are published *only* if the database transaction succeeds, preventing data inconsistency.

---

### 📝 Observability

#### [`zeabay-logging`](./zeabay-logging/README.md)
- **Purpose**: Aspect-oriented log tracing.
- **Technology**: AspectJ, SLF4J/MDC.
- **Usage**: Add `@Loggable` to any class or method.
- **Effect**: Automatically logs method entry/exit, arguments, results, and duration with full Trace ID context—even in complex reactive pipelines.

---

## 🛠️ Development & Standards

### Quality Gate
The project uses **Spotless** with **Google Java Format** to maintain consistent code style.
```bash
mvn spotless:apply
```

### Build Requirements
- **JDK 25**
- **Maven 3.9+**

---

**Maintainer:** [Zeynel Abiddin Aydar](https://zeynelaydar.com) - zeynelaydar@gmail.com
