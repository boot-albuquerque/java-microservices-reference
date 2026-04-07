# ADR-001: Spring Boot 3 over Quarkus

**Status:** Accepted
**Date:** 2026-04-07
**Author:** Lucas Albuquerque

---

## Context

Choosing the primary framework for Java 21 microservices in a banking environment with 50+ engineers across 6 tribes. The framework must support high-concurrency workloads (50k+ req/min), integrate with existing Spring-based services, and be adoptable by the entire organization.

## Decision Factors

- Team expertise (90% Spring experience, <10% Quarkus)
- Ecosystem maturity (security, data, cloud, messaging)
- Virtual Threads (Java 21) support
- Migration path from existing Spring Boot 2.x services
- Long-term support and community

## Options Considered

### Option 1: Spring Boot 3.3

**Pros:**
- Largest Java framework ecosystem (Spring Security, Data, Cloud, Kafka)
- 90% of engineers already proficient — zero ramp-up
- First-class Virtual Threads support since 3.2
- Direct migration path from Spring Boot 2.x services
- SpringDoc OpenAPI, Actuator, Micrometer built-in
- Enterprise support (VMware/Broadcom)

**Cons:**
- Slower cold start (~2s vs ~0.5s Quarkus native)
- Higher base memory (~200MB vs ~50MB native)
- GraalVM native compilation possible but less mature than Quarkus

**Complexity:** Low (team already knows it)

### Option 2: Quarkus 3.x

**Pros:**
- Fastest startup (sub-second with native compilation)
- Lowest memory footprint (~50MB native)
- CDI-based, familiar to Java EE developers
- Excellent developer experience (live reload)

**Cons:**
- Only 2 engineers have Quarkus experience — weeks of ramp-up for 50+ engineers
- Smaller ecosystem (fewer integrations, less StackOverflow coverage)
- GraalVM native compilation adds CI/CD complexity
- Less mature Spring Security equivalent
- Migration from Spring Boot 2.x services would require full rewrite

**Complexity:** High (team ramp-up + ecosystem gaps)

## Decision

**Spring Boot 3.3** — the ecosystem maturity, team expertise, and Virtual Threads support make the startup/memory trade-off acceptable for long-running banking services.

## Consequences

**Positive:**
- Zero ramp-up time for 90% of engineers
- Seamless migration from Spring Boot 2.x services
- Rich ecosystem covers all requirements (security, messaging, observability)
- RFC template adopted by 14 services in first quarter

**Negative:**
- Cold start ~2s (acceptable for long-running services, not for serverless)
- Higher memory baseline (mitigated by Virtual Threads reducing thread stack overhead)

**Validation:**
- Benchmark: 50k req/min with p99 < 200ms on 1GB heap (Virtual Threads)
- 14 services created from this template in Q1 with zero framework issues
