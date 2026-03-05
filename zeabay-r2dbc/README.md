# Zeabay R2DBC

The `zeabay-r2dbc` module provides reactive database support, standardizing entity baseline and automated auditing for the Zeabay ecosystem.

## 🛠️ Technology Stack
- **Spring Data R2DBC**: Reactive relational database access.
- **R2DBC**: Reactive database drivers.
- **Project Reactor**: Asynchronous data streams.

## 📦 Core Components

### 1. `BaseEntity`
A baseline class for all domain entities. It provides standard fields:
- `id`: The primary key (usually TSID).
- `createdAt`: Set automatically when the record is created.
- `updatedAt`: Updated automatically on every save.
- `version`: Optimistic locking field to prevent concurrent update conflicts.

### 2. `ZeabayR2dbcAuditingAutoConfiguration`
Automatically enables R2DBC auditing and registers an `AuditorAware` bean to track *who* created or modified a record.

## 🚀 How to Use

### 1. Define your Entity
Extend `BaseEntity` to inherit standard auditing and ID fields:

```java
@Table("customers")
@Data
@EqualsAndHashCode(callSuper = true)
public class Customer extends BaseEntity {
    private String name;
    private String email;
}
```

### 2. Repository Usage
Use standard Spring Data R2DBC repositories. The auditing fields will be handled by the library.

```java
public interface CustomerRepository extends ReactiveCrudRepository<Customer, Long> {
}
```

## ⚠️ System Impact
- **Consistency**: All tables across all microservices will have the same metadata structure.
- **Data Integrity**: Built-in optimistic locking (`@Version`) prevents "lost update" scenarios.
- **Observability**: Simplifies tracking when and by whom data was changed.
