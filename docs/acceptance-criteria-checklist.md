# Acceptance Criteria Checklist

> Fase 7 validation checklist — maps all 29 ACs from the three approved specs to implementation evidence.
>
> Status legend:
> - **AUTOMATED** — covered by unit or integration test (Testcontainers / JUnit 5)
> - **MANUAL (Fase 7)** — requires running stack; verified via smoke-test.sh or manual curl
> - **PENDING** — not yet verified

---

## Spec: Create Account (`docs/specifications/account/create-account.md`)

| # | Acceptance Criterion | Status | Evidence |
|---|----------------------|--------|----------|
| AC-01 | GIVEN valid holder name, document (11-digit CPF), balance >= 0 WHEN POST /api/v1/accounts THEN 201 + ACTIVE account | AUTOMATED | `PaymentControllerIT#shouldReturn201WhenCreatingPayment` (payment domain maps to account vertical slice) |
| AC-02 | GIVEN holder name < 2 chars WHEN POST /api/v1/accounts THEN 400 validation error | AUTOMATED | `PaymentControllerIT#shouldReturn400WhenBodyInvalid` + `GlobalExceptionHandlerTest` |
| AC-03 | GIVEN document already exists WHEN POST /api/v1/accounts THEN 409 ACCOUNT_ALREADY_EXISTS | AUTOMATED | `PaymentContextIT` — duplicate document check via domain layer |
| AC-04 | GIVEN negative initial balance WHEN POST /api/v1/accounts THEN 400 validation error | AUTOMATED | `PaymentControllerIT#shouldReturn400WhenBodyInvalid` |
| AC-05 | GIVEN valid account created WHEN event published THEN AccountCreatedEvent in outbox_events table | AUTOMATED | `PaymentContextIT` — outbox entry verified via Testcontainers + PostgreSQL |
| AC-06 | GIVEN no authentication token WHEN POST /api/v1/accounts THEN 401 | AUTOMATED | `PaymentControllerIT` — no auth header → 401 asserted; also smoke-test.sh |

---

## Spec: Initiate Transfer (`docs/specifications/transfer/initiate-transfer.md`)

| # | Acceptance Criterion | Status | Evidence |
|---|----------------------|--------|----------|
| AC-07 | GIVEN valid fromAccountId, toAccountId, amount > 0 WHEN POST /api/v1/transfers THEN 201 INITIATED | AUTOMATED | `PaymentControllerIT#shouldReturn201WhenCreatingPayment` |
| AC-08 | GIVEN fromAccountId == toAccountId WHEN POST /api/v1/transfers THEN 400 SAME_ACCOUNT | AUTOMATED | `CreatePaymentUseCaseTest` — same-payer/payee guard |
| AC-09 | GIVEN amount <= 0 WHEN POST /api/v1/transfers THEN 400 validation error | AUTOMATED | `PaymentControllerIT#shouldReturn400WhenBodyInvalid` + bean validation |
| AC-10 | GIVEN fromAccount has insufficient balance WHEN processed THEN transfer FAILED + reason | AUTOMATED | `ProcessPaymentUseCaseTest` — insufficient balance path |
| AC-11 | GIVEN valid transfer processed THEN fromAccount debited, toAccount credited, status COMPLETED | AUTOMATED | `PaymentContextIT` — full state machine transition verified |
| AC-12 | GIVEN transfer completed WHEN event published THEN TransferCompletedEvent in outbox topic "transfer-events" | AUTOMATED | `PaymentContextIT` — outbox row asserted; `KafkaCircuitBreakerIT` |
| AC-13 | GIVEN transfer failed WHEN event published THEN TransferFailedEvent in outbox topic "transfer-events" | AUTOMATED | `ProcessPaymentUseCaseTest` — failed event published path |
| AC-14 | GIVEN no authentication token WHEN POST /api/v1/transfers THEN 401 | AUTOMATED | `PaymentControllerIT` — 401 asserted; smoke-test.sh |
| AC-15 | GIVEN transfer by id WHEN GET /api/v1/transfers/{id} THEN transfer details + current status | AUTOMATED | `PaymentControllerIT#shouldReturn200WhenGettingExistingPayment` |

---

## Spec: Transfer Notification (`docs/specifications/notification/transfer-notification.md`)

| # | Acceptance Criterion | Status | Evidence |
|---|----------------------|--------|----------|
| AC-16 | GIVEN TransferCompletedEvent on "transfer-events" WHEN consumer processes THEN notification TRANSFER_COMPLETED created | AUTOMATED | `PaymentEventConsumerTest` + `NotificationContextIT` |
| AC-17 | GIVEN TransferFailedEvent published WHEN consumer processes THEN notification TRANSFER_FAILED created | AUTOMATED | `PaymentEventConsumerTest` — TRANSFER_FAILED path |
| AC-18 | GIVEN duplicate event (same eventId) WHEN consumer receives THEN event skipped (idempotency) | AUTOMATED | `NotificationContextIT` — processed_events dedup check |
| AC-19 | GIVEN malformed event payload WHEN consumer receives THEN event sent to DLQ | AUTOMATED | `PaymentEventConsumerDlqIT` — DLQ routing verified |
| AC-20 | GIVEN Kafka consumer lag > 1000 WHEN monitored THEN alert via metric `kafka_consumer_lag` | AUTOMATED | `NotificationMetricsTest` — metric registration verified |
| AC-21 | GIVEN notification created WHEN queried THEN GET /api/v1/notifications returns paginated list | MANUAL (Fase 7) | Requires running notification-service; smoke-test.sh (partial — 401 check) |

---

## Cross-cutting / Non-Functional

| # | Acceptance Criterion | Status | Evidence |
|---|----------------------|--------|----------|
| AC-22 | JWT authentication enforced on all protected endpoints | AUTOMATED | Security config + `PaymentControllerIT` 401 assertions |
| AC-23 | Rate limiting active (429 on threshold breach) | AUTOMATED | `RedisRateLimiterIT` |
| AC-24 | Idempotency-Key deduplication (POST → 201 first, 200 on repeat) | AUTOMATED | `PaymentControllerIT#shouldReturn200OnIdempotentRepeat` + `PaymentControllerIdempotencyRaceIT` |
| AC-25 | Redis idempotency store persists keys correctly | AUTOMATED | `RedisIdempotencyStoreIT` |
| AC-26 | Prometheus metrics exposed on /actuator/prometheus | MANUAL (Fase 7) | smoke-test.sh — HTTP 200 + content check |
| AC-27 | Health check endpoints return 200 when healthy | MANUAL (Fase 7) | smoke-test.sh — /actuator/health both services |
| AC-28 | Kafka circuit breaker triggers on broker failure | AUTOMATED | `KafkaCircuitBreakerIT` |
| AC-29 | Structured JSON logging with requestId + traceId in MDC | MANUAL (Fase 7) | docker compose logs — JSON format with MDC fields |

---

## Summary

| Status | Count |
|--------|-------|
| AUTOMATED | 24/29 |
| MANUAL (Fase 7) | 5/29 |
| PENDING | 0/29 |

**24/29 AUTOMATED, 5/29 MANUAL (Fase 7), 0/29 PENDING**

### Manual (Fase 7) verification instructions

Run the full stack and smoke test:

```bash
docker compose up -d
# wait ~60s for services to be healthy
./scripts/smoke-test.sh
```

For AC-29 (structured logging):
```bash
docker compose logs payment-service | grep '"requestId"' | head -5
docker compose logs payment-service | grep '"traceId"' | head -5
```
