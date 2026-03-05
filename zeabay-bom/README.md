# Zeabay BOM (Bill of Materials)

The `zeabay-bom` module is a Maven Bill of Materials (BOM) that centralizes dependency management for the entire Zeabay ecosystem. It ensures that all microservices use the same, compatible versions of internal and external libraries.

## 🛠️ Technology Stack
- **Maven**: POM-based dependency management.
- **Spring Boot**: 4.0.2 (Parent inheritance).

## 📋 What it Solves
- **Version Hell**: Prevents version mismatches between different microservices.
- **Simplified Upgrades**: Upgrading a version in the BOM automatically propagates to all services importing it.
- **Consistent Tech Stack**: Ensures every service uses the same version of libraries like `tsid`, `springdoc`, and `mapstruct`.

## 🚀 How to Use

### 1. Import in Parental BOM or Root POM
Import this BOM in the `<dependencyManagement>` section of your microservice's root `pom.xml`:

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.zeabay</groupId>
            <artifactId>zeabay-bom</artifactId>
            <version>${zeabay-common.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. Add Dependencies Without Versions
After importing, you can add any `zeabay-common` module or managed third-party library without specifying the version:

```xml
<dependencies>
    <dependency>
        <groupId>com.zeabay</groupId>
        <artifactId>zeabay-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
    </dependency>
</dependencies>
```

## ⚠️ System Impact
- **Build-time only**: This module has no runtime impact; it is purely for Maven dependency resolution.
- **Maintainability**: Significantly reduces the effort required to keep the ecosystem synchronized.
