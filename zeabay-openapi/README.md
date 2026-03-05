# Zeabay OpenAPI

The `zeabay-openapi` module provides automated Swagger/OpenAPI documentation for Zeabay microservices, ensuring that API contracts are always up-to-date and easily accessible.

## 🛠️ Technology Stack
- **SpringDoc OpenAPI**: 3.0.1+
- **Swagger UI**: Integrated web interface.

## 📦 Core Components

### 1. `ZeabayOpenApiAutoConfiguration`
Automatically configures the OpenAPI bean with custom metadata (Title, Version, Description) and sets up the Swagger UI and API Docs paths.

## 🚀 How to Use

Simply include the dependency in your `pom.xml`. The library will automatically scan your `@RestController` classes and generate the documentation.

```xml
<dependency>
    <groupId>com.zeabay</groupId>
    <artifactId>zeabay-openapi</artifactId>
</dependency>
```

### Accessing the Docs
- **Swagger UI**: `http://localhost:port/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:port/v3/api-docs`

## ⚙️ Configuration (application.yml)
You can customize the base path or disable it:
```yaml
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

## ⚠️ System Impact
- **Developer Productivity**: Provides a live interface to test API endpoints without external tools like Postman.
- **Contract Accuracy**: Ensures the documentation always matches the actual code implementation.
- **Overhead**: Minor increase in startup time due to classpath scanning for controllers.
