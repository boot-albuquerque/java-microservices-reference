# ADR-002: Virtual Threads over WebFlux

**Status:** Accepted
**Date:** 2026-04-07
**Author:** Lucas Albuquerque

---

## Context

Banking services handle 50k+ req/min with blocking I/O calls to PostgreSQL, Redis, and Kafka. The traditional thread-per-request model (Tomcat default: 200 threads) becomes a bottleneck under high concurrency. We need a concurrency model that scales without sacrificing code readability.

## Decision Factors

- Code simplicity and debuggability
- Team adoption speed (8-engineer squads)
- I/O-bound workload profile (database, cache, messaging)
- Compatibility with existing Spring ecosystem
- Production observability (stack traces, profiling)

## Options Considered

### Option 1: Virtual Threads (Project Loom, Java 21)

```java
// Simple imperative code — blocks a Virtual Thread, NOT a platform thread
@GetMapping("/{id}")
public ApiResponse<AccountResponse> findById(@PathVariable UUID id) {
    var account = accountService.findById(id);  // Blocks virtual thread during DB I/O
    return ApiResponse.ok(AccountResponse.from(account));
}
```

**Pros:**
- Imperative code — same style as traditional Java
- Stack traces are readable and useful for debugging
- One config line: `spring.threads.virtual.enabled=true`
- Works with existing Spring Data JPA, Spring Kafka, etc.
- Engineers adopt in hours, not weeks

**Cons:**
- Requires Java 21+ (no backport)
- `synchronized` blocks pin virtual threads (use `ReentrantLock`)
- Some libraries not yet compatible (check before adopting)
- CPU-bound workloads don't benefit

**Complexity:** Low

### Option 2: WebFlux (Spring Reactive / Project Reactor)

```java
// Reactive code — non-blocking but complex
@GetMapping("/{id}")
public Mono<ApiResponse<AccountResponse>> findById(@PathVariable UUID id) {
    return accountService.findById(id)
        .map(AccountResponse::from)
        .map(ApiResponse::ok)
        .onErrorResume(AccountException.NotFound.class,
            e -> Mono.error(new ResponseStatusException(NOT_FOUND)));
}
```

**Pros:**
- Proven non-blocking model
- Backpressure built-in
- Works on Java 17+

**Cons:**
- Steep learning curve (Mono/Flux operators, reactive mindset)
- Stack traces are nearly useless (reactor scheduler noise)
- Requires reactive drivers for EVERYTHING (R2DBC, reactive Redis, reactive Kafka)
- Debugging is significantly harder
- 3-6 weeks ramp-up for a squad of 8

**Complexity:** High

## Decision

**Virtual Threads** — enabled via `spring.threads.virtual.enabled=true`. Simple imperative code with massive concurrency, no reactive complexity.

## Consequences

**Positive:**
- p99 latency dropped from 800ms to 180ms under 50k req/min in production
- Zero code changes needed — just the config flag
- Engineers adopted in days, not weeks
- Stack traces remain readable for debugging
- Works with all existing Spring ecosystem (JPA, Kafka, Redis)

**Negative:**
- Must avoid `synchronized` (use `ReentrantLock` or `java.util.concurrent`)
- Thread-local variables need care (virtual threads may not inherit them)
- Java 21 is minimum requirement (no Java 17 fallback)

**Validation:**
- Load test: 50k req/min, p99 < 200ms, 0% error rate on 3 replicas (1GB heap each)
- Compared to WebFlux benchmark: equivalent throughput, 60% less code complexity
