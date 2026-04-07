# ADR-004: Apache Kafka over RabbitMQ

**Status:** Accepted
**Date:** 2026-04-07
**Author:** Lucas Albuquerque

---

## Context

Inter-service communication for domain events (AccountCreated, TransferCompleted, etc.). The messaging system must support at-least-once delivery, event replay for debugging, and handle 7M+ events/day in production.

## Decision Factors

- Event replay capability (audit, debugging, reprocessing)
- Throughput (7M+ events/day)
- Consumer group semantics (multiple services consuming same events)
- Spring ecosystem integration
- Operational experience

## Options Considered

### Option 1: Apache Kafka 3.x

**Pros:**
- Persistent log — events stored for days/weeks, enabling replay
- Consumer groups — multiple services consume independently
- High throughput (millions of events/day trivially)
- Partitioned topics for parallelism
- Spring Kafka mature integration
- Event replay for debugging production issues

**Cons:**
- Higher operational complexity (ZooKeeper/KRaft, partitions, offsets)
- Not ideal for request-reply patterns
- Minimum 3 brokers for production (resource cost)
- Consumer lag monitoring required

**Complexity:** Medium

### Option 2: RabbitMQ 3.x

**Pros:**
- Simpler to operate (single node sufficient for moderate load)
- Built-in routing patterns (fanout, topic, headers)
- Lower latency for small messages
- Good for request-reply (RPC pattern)

**Cons:**
- No event replay (messages are consumed and gone)
- Consumer competing pattern by default (not consumer groups)
- Throughput ceiling lower than Kafka for high-volume scenarios
- No persistent log for audit/debugging

**Complexity:** Low

## Decision

**Apache Kafka 3.x** — event replay, consumer groups, and high throughput are essential for banking event-driven architecture. The operational complexity is justified.

## Consequences

**Positive:**
- Event replay enables debugging production incidents without data loss
- Consumer groups allow Transfer, Notification, and Analytics services to consume independently
- 7M+ events/day handled without performance concerns
- Spring Kafka provides `@KafkaListener`, error handlers, DLQ out of the box

**Negative:**
- Minimum 3 brokers in production (use single broker in dev via docker-compose)
- Consumer lag monitoring required (Prometheus + Grafana dashboard)
- Message ordering guaranteed only within partitions (partition by aggregate ID)
