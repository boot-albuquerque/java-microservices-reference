# ADR-006: Clean Architecture with Domain Layer Isolation

**Status:** Accepted
**Date:** 2026-04-07
**Author:** Lucas Albuquerque

---

## Context

In most Spring Boot projects, JPA annotations (`@Entity`, `@Table`, `@Column`) live alongside business logic in the same class. This creates tight coupling between domain and persistence — changing the database requires changing domain code, and domain tests need Spring context.

## Options Considered

### Option 1: Separated Domain + JPA Entities (Clean Architecture)

Domain entities are plain Java classes. JPA entities live in `infrastructure/persistence/` with `toDomain()` / `fromDomain()` mappers.

```
account/
├── domain/
│   ├── Account.java              # Pure Java — business logic
│   ├── AccountRepository.java    # Interface (port)
│   └── exception/
└── infrastructure/
    └── persistence/
        ├── AccountJpaEntity.java        # @Entity — persistence mapping
        └── JpaAccountRepository.java    # Spring Data implementation
```

**Pros:**
- Domain testable without Spring context (plain JUnit, fast)
- Can swap persistence (JPA → jOOQ → JDBC) without touching domain
- Business logic is clear, uncluttered by annotations
- Forces clean separation of concerns

**Cons:**
- Mapping overhead (domain ↔ JPA entity conversion)
- More files per bounded context (~2x)
- New developers need to understand the separation

### Option 2: Single Entity (Spring Boot Default)

One class serves as both domain entity and JPA entity.

```java
@Entity @Table(name = "accounts")
public class Account {
    @Id private UUID id;
    @Column private String holderName;

    public void withdraw(BigDecimal amount) { /* business logic */ }
}
```

**Pros:**
- Fewer files
- Faster to write initially
- Spring Boot default pattern (tutorials, guides)

**Cons:**
- Domain tests require Spring context or H2 (slow)
- Business logic mixed with persistence annotations
- Changing database requires touching domain classes
- JPA proxy behavior can affect domain logic (lazy loading surprises)

## Decision

**Clean Architecture with separated domain and JPA entities.** The mapping overhead is worth the testability and clean separation.

## Consequences

**Positive:**
- Domain tests run in milliseconds (no Spring context, no database)
- Domain coverage >=90% is achievable and fast to maintain
- Infrastructure can change without touching business logic
- Forces developers to think about domain boundaries

**Negative:**
- ~2x more files per bounded context
- Mapping code between domain and JPA entities
- New team members need onboarding on the pattern

**Validation:**
- Domain test suite runs in <2s (vs ~30s with @SpringBootTest)
- Domain coverage consistently >=90% across all contexts
- Persistence swap demonstrated: JPA → plain JDBC for one query (zero domain changes)
