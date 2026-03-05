# Zeabay R2DBC

The `zeabay-r2dbc` module provides reactive database support, standardizing entity baseline and automated auditing for the Zeabay ecosystem.

## 🛠️ Technology Stack
- **Spring Data R2DBC**: Reactive relational database access.
- **R2DBC**: Reactive database drivers.
- **Project Reactor**: Asynchronous data streams.
- **TSID**: Time-Sorted Unique Identifiers for primary keys.

## 📦 Core Components

### 1. `BaseEntity`
A baseline abstract class for domain entities that require a full audit trail. It provides:
- `id` (`Long`): TSID primary key, auto-assigned on INSERT via `zeabayTsidBeforeConvertCallback`.
- `createdAt` / `updatedAt`: Populated automatically by Spring Data R2DBC auditing.
- `createdBy` / `updatedBy`: Set from the reactive security context (or `"system"` as fallback).
- `deletedAt` / `deletedBy`: Soft-delete support via `isDeleted()` helper.

### 2. `ZeabayR2dbcAuditingAutoConfiguration`
Activates R2DBC auditing and registers three beans:

| Bean | Type | Purpose |
|---|---|---|
| `zeabayReactiveAuditorAware` | `ReactiveAuditorAware<String>` | Returns `"system"` when no security context is present (overridden by `zeabay-security`). |
| `zeabayTsidBeforeConvertCallback` | `BeforeConvertCallback<BaseEntity>` | Assigns a TSID Long to `BaseEntity.id` before INSERT if null. Only fires for `BaseEntity` subclasses. |
| `zeabayGenericTsidBeforeConvertCallback` | `BeforeConvertCallback<Object>` | Fires for every entity type. Immediately returns no-op for `BaseEntity` instances (`instanceof` guard); otherwise assigns a TSID Long to any `@Id Long` field via reflection before INSERT if null. |

The generic callback enables entities like `AuthVerificationToken` (which don't need audit columns) to still receive automatic TSID primary keys without inheriting from `BaseEntity`.

## 🚀 How to Use

### Option A — Entity with full audit trail
Extend `BaseEntity` to inherit the TSID PK and all audit/soft-delete fields:

```java
@Table("customers")
@Getter
@Setter
public class Customer extends BaseEntity {
    private String name;
    private String email;
}
```

### Option B — Lightweight entity (no audit columns)
Declare a plain class with an `@Id Long` field. The generic TSID callback will assign the ID automatically:

```java
@Table("verification_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {

    /** TSID assigned by zeabayGenericTsidBeforeConvertCallback before INSERT. */
    @Id
    private Long id;

    private String token;
    private Instant expiresAt;
}
```

### Repository
Standard Spring Data R2DBC repositories work with both options:

```java
public interface CustomerRepository extends ReactiveCrudRepository<Customer, Long> {
}
```

## ⚠️ System Impact
- **Consistency**: All `BaseEntity` tables have the same audit metadata structure.
- **Flexibility**: Lightweight entities still get TSID primary keys without inheriting audit columns.
- **Observability**: Simplifies tracking when and by whom data was changed.
