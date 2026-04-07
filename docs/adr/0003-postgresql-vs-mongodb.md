# ADR-003: PostgreSQL over MongoDB

**Status:** Accepted
**Date:** 2026-04-07
**Author:** Lucas Albuquerque

---

## Context

Selecting the primary datastore for banking microservices that process financial transactions (account balances, transfers, audit trails). Data integrity is non-negotiable — a lost or duplicated transaction is a regulatory incident.

## Decision Factors

- ACID transactions (mandatory for financial operations)
- Auditability and compliance (banking regulations, LGPD)
- Query flexibility (complex reporting, aggregations)
- Operational maturity (monitoring, backup, recovery)
- Team expertise

## Options Considered

### Option 1: PostgreSQL 16

**Pros:**
- Full ACID transactions with serializable isolation
- JSONB for flexible metadata when needed (settings, preferences)
- Mature tooling: `pg_stat_statements`, `EXPLAIN ANALYZE`, `pg_dump`
- Row-Level Security for multi-tenancy if needed
- Flyway for versioned, auditable schema migrations
- Spring Data JPA + Hibernate have first-class support
- 100% of team has PostgreSQL experience

**Cons:**
- Schema migrations required for every structural change
- Horizontal scaling more complex than MongoDB (Citus/partitioning)
- No native document model (JSONB is a workaround)

**Complexity:** Low

### Option 2: MongoDB 7.x

**Pros:**
- Flexible document model (no schema migrations)
- Native horizontal scaling (sharding)
- Good for event store / audit log patterns

**Cons:**
- No true multi-document ACID until recent versions (and still has limitations)
- Banking compliance auditors expect relational databases
- Spring Data MongoDB has fewer features than Spring Data JPA
- Aggregation pipeline is powerful but hard to maintain
- Only 2 engineers have MongoDB production experience
- No Flyway equivalent (schema drift risk)

**Complexity:** Medium

## Decision

**PostgreSQL 16** — ACID guarantees, regulatory compliance, team expertise, and Spring Data JPA maturity make it the clear choice for banking transactions.

## Consequences

**Positive:**
- Full ACID for all financial operations
- JSONB available when flexibility needed (account settings, metadata)
- Flyway ensures schema changes are versioned, reviewed, and auditable
- Compliance auditors are familiar with PostgreSQL
- HikariCP connection pooling works out of the box with Spring Boot

**Negative:**
- Schema migrations required for every structural change (mitigated by Flyway)
- Horizontal scaling requires Citus or application-level partitioning
- No native change streams (use Transactional Outbox + Kafka instead)

**Validation:**
- Query performance: p99 < 10ms for indexed lookups on 10M+ rows
- Connection pool: 20 connections handle 5k concurrent requests with Virtual Threads
