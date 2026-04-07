# ADR-005: Transactional Outbox Pattern for Event Publishing

**Status:** Accepted
**Date:** 2026-04-07
**Author:** Lucas Albuquerque

---

## Context

When a service writes to the database AND publishes an event to Kafka, either operation can fail independently — creating inconsistency:

- **Scenario A:** DB commits, Kafka publish fails → event lost, consumers never know
- **Scenario B:** Kafka publishes, DB rolls back → phantom event, consumers act on invalid data

Both scenarios are unacceptable in banking. We need atomic consistency between state changes and event publishing.

## Options Considered

### Option 1: Transactional Outbox

Write the event to an `outbox_events` table in the SAME database transaction as the domain change. A separate processor polls the outbox and publishes to Kafka.

```java
@Transactional
public Account handle(CreateAccountCommand command) {
    var account = Account.create(command.holderName(), command.holderDocument(), command.initialBalance());
    accountRepository.save(account);

    // SAME transaction — atomic!
    outboxRepository.save(new OutboxEvent(
        "account-events", "AccountCreated",
        objectMapper.writeValueAsString(AccountCreatedEvent.from(account))
    ));
    return account;
}
```

**Pros:**
- Atomic consistency (DB + event in one transaction)
- Events survive Kafka downtime (stored in DB until published)
- Simple to implement with Spring `@Transactional`
- Auditability (outbox table is an event log)

**Cons:**
- Slightly higher latency (polling interval of outbox processor)
- Additional table and processor component
- At-least-once delivery (consumers must be idempotent)

### Option 2: Dual Write (Direct Publish)

Write to DB and publish to Kafka as separate operations.

**Pros:**
- Lower latency (no polling)
- Simpler (no outbox table)

**Cons:**
- Not atomic — either can fail independently
- Data loss or phantom events possible
- Unacceptable for banking transactions

### Option 3: Change Data Capture (Debezium)

Capture database changes from WAL and publish to Kafka automatically.

**Pros:**
- No application code changes
- Captures all DB changes

**Cons:**
- Additional infrastructure (Debezium + Kafka Connect)
- Less control over event schema
- CDC events are DB-schema-coupled, not domain events
- Operational complexity significantly higher

## Decision

**Transactional Outbox Pattern** — atomic consistency is non-negotiable for banking. The polling latency trade-off is acceptable.

## Consequences

**Positive:**
- Zero data loss between DB state and events
- Events survive Kafka downtime
- Outbox table doubles as audit log
- Simple implementation with Spring `@Transactional`

**Negative:**
- Outbox processor adds ~100ms latency (polling interval)
- Consumers MUST be idempotent (at-least-once delivery)
- Outbox table needs periodic cleanup (delete published events > 7 days)

## Implementation

**Schema:**
```sql
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_key VARCHAR(255),
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;
```

**Processor:** Scheduled task (`@Scheduled`) that polls unpublished events and sends to Kafka, marking `published_at` on success.
