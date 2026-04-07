# Architecture Decision Records (ADRs)

> **Architecture Decision Records — document significant technical decisions.**

## Index

| # | Decision | Status | Impact |
|---|----------|--------|--------|
| [0001](0001-spring-boot-3-vs-quarkus.md) | Spring Boot 3 over Quarkus | Accepted | Framework choice |
| [0002](0002-virtual-threads-vs-webflux.md) | Virtual Threads over WebFlux | Accepted | Concurrency model |
| [0003](0003-postgresql-vs-mongodb.md) | PostgreSQL over MongoDB | Accepted | Data store |
| [0004](0004-kafka-vs-rabbitmq.md) | Apache Kafka over RabbitMQ | Accepted | Messaging |
| [0005](0005-transactional-outbox-pattern.md) | Transactional Outbox for events | Accepted | Event consistency |
| [0006](0006-clean-architecture-domain-isolation.md) | Clean Architecture domain isolation | Accepted | Code structure |

## When to Write an ADR

- Introducing a new technology or library
- Changing the database schema significantly
- Adding a new bounded context
- Changing communication patterns (sync → async)
- Any decision affecting multiple services or the template itself

**Do NOT write an ADR for:**
- Adding a standard CRUD endpoint within existing patterns
- Minor refactoring
- Bug fixes

## Template

Use the ADR format shown in existing ADRs above: Context, Options Considered (with Pros/Cons), Decision, Consequences, Validation.

## Naming

`NNNN-{kebab-case-title}.md` — sequential numbering, never reuse numbers.
Next available: **0007**
