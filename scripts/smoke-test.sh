#!/usr/bin/env bash
# Smoke test for Java Microservices Reference — requires running stack (docker compose up)
set -euo pipefail

# Load host port overrides from .env if present
if [[ -f "$(dirname "$0")/../.env" ]]; then
  set -o allexport
  # shellcheck disable=SC1091
  source "$(dirname "$0")/../.env"
  set +o allexport
fi

PORT_PAYMENT="${PORT_PAYMENT:-18081}"
PORT_NOTIFICATION="${PORT_NOTIFICATION:-18082}"
PORT_KAFDROP="${PORT_KAFDROP:-19000}"

PAYMENT_URL="${PAYMENT_URL:-http://localhost:${PORT_PAYMENT}}"
NOTIFICATION_URL="${NOTIFICATION_URL:-http://localhost:${PORT_NOTIFICATION}}"
JWT_SECRET="${JWT_SECRET:-dev-secret-change-in-prod-min-32-bytes-xxxx}"

PASS=0
FAIL=0

pass() { echo "[PASS] $1"; PASS=$((PASS+1)); }
fail() { echo "[FAIL] $1"; FAIL=$((FAIL+1)); }

# Generate a HS256 JWT using openssl (no external deps)
generate_jwt() {
  local user_id="${1:-$(uuidgen | tr '[:upper:]' '[:lower:]')}"
  local header
  local payload
  local secret="$JWT_SECRET"

  header=$(printf '{"alg":"HS256","typ":"JWT"}' | openssl base64 -e -A | tr '+/' '-_' | tr -d '=')
  # exp: now + 3600
  local exp=$(( $(date +%s) + 3600 ))
  payload=$(printf '{"userId":"%s","iat":%d,"exp":%d}' "$user_id" "$(date +%s)" "$exp" \
    | openssl base64 -e -A | tr '+/' '-_' | tr -d '=')

  local sig
  sig=$(printf '%s.%s' "$header" "$payload" \
    | openssl dgst -sha256 -hmac "$secret" -binary \
    | openssl base64 -e -A | tr '+/' '-_' | tr -d '=')

  printf '%s.%s.%s' "$header" "$payload" "$sig"
}

echo "=== Java Microservices Reference — Smoke Test ==="
echo "Payment:      $PAYMENT_URL"
echo "Notification: $NOTIFICATION_URL"
echo ""

# ── Health checks ─────────────────────────────────────────────────────────────
echo "--- Health checks ---"

status=$(curl -s -o /dev/null -w "%{http_code}" "$PAYMENT_URL/actuator/health")
if [[ "$status" == "200" ]]; then
  pass "payment-service /actuator/health → 200"
else
  fail "payment-service /actuator/health → $status (expected 200)"
fi

status=$(curl -s -o /dev/null -w "%{http_code}" "$NOTIFICATION_URL/actuator/health")
if [[ "$status" == "200" ]]; then
  pass "notification-service /actuator/health → 200"
else
  fail "notification-service /actuator/health → $status (expected 200)"
fi

# ── Auth: 401 without token ────────────────────────────────────────────────────
echo ""
echo "--- Auth assertions ---"

status=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "$PAYMENT_URL/api/v1/payments" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"amount":"10.00","currency":"BRL","payerId":"00000000-0000-0000-0000-000000000001","payeeId":"00000000-0000-0000-0000-000000000002"}')
if [[ "$status" == "401" ]]; then
  pass "POST /api/v1/payments without token → 401"
else
  fail "POST /api/v1/payments without token → $status (expected 401)"
fi

# ── Payment: POST → 201 ────────────────────────────────────────────────────────
echo ""
echo "--- Payment CRUD ---"

TOKEN=$(generate_jwt "$(uuidgen | tr '[:upper:]' '[:lower:]')")
IDEMPOTENCY_KEY=$(uuidgen | tr '[:upper:]' '[:lower:]')
PAYER_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')
PAYEE_ID=$(uuidgen | tr '[:upper:]' '[:lower:]')

RESPONSE=$(curl -s -w "\n%{http_code}" \
  -X POST "$PAYMENT_URL/api/v1/payments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "{\"amount\":\"500.00\",\"currency\":\"BRL\",\"payerId\":\"$PAYER_ID\",\"payeeId\":\"$PAYEE_ID\"}")

BODY=$(echo "$RESPONSE" | sed '$d')
status=$(echo "$RESPONSE" | tail -n1)

if [[ "$status" == "201" ]]; then
  pass "POST /api/v1/payments → 201"
else
  fail "POST /api/v1/payments → $status (expected 201)"
fi

PAYMENT_ID=$(echo "$BODY" | grep -o '"id":"[^"]*"' | awk 'NR==1' | cut -d'"' -f4)

# ── Payment: GET by ID ─────────────────────────────────────────────────────────
if [[ -n "$PAYMENT_ID" ]]; then
  status=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $TOKEN" \
    "$PAYMENT_URL/api/v1/payments/$PAYMENT_ID")
  if [[ "$status" == "200" ]]; then
    pass "GET /api/v1/payments/$PAYMENT_ID → 200"
  else
    fail "GET /api/v1/payments/$PAYMENT_ID → $status (expected 200)"
  fi
else
  fail "GET /api/v1/payments/{id} — could not extract payment id from POST response"
fi

# ── Payment: 400 invalid body ─────────────────────────────────────────────────
status=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "$PAYMENT_URL/api/v1/payments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $(uuidgen | tr '[:upper:]' '[:lower:]')" \
  -d '{"currency":"BRL"}')
if [[ "$status" == "400" ]]; then
  pass "POST /api/v1/payments with missing fields → 400"
else
  fail "POST /api/v1/payments with missing fields → $status (expected 400)"
fi

# ── Idempotency: repeat same key → 200 ────────────────────────────────────────
echo ""
echo "--- Idempotency ---"

status=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "$PAYMENT_URL/api/v1/payments" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $IDEMPOTENCY_KEY" \
  -d "{\"amount\":\"500.00\",\"currency\":\"BRL\",\"payerId\":\"$PAYER_ID\",\"payeeId\":\"$PAYEE_ID\"}")
if [[ "$status" == "200" ]]; then
  pass "POST /api/v1/payments idempotent repeat → 200"
else
  fail "POST /api/v1/payments idempotent repeat → $status (expected 200)"
fi

# ── Metrics endpoint ───────────────────────────────────────────────────────────
echo ""
echo "--- Observability ---"

status=$(curl -s -o /dev/null -w "%{http_code}" "$PAYMENT_URL/actuator/prometheus")
if [[ "$status" == "200" ]]; then
  pass "payment-service /actuator/prometheus → 200"
else
  fail "payment-service /actuator/prometheus → $status (expected 200)"
fi

# Check for custom metric in prometheus output
metrics=$(curl -s "$PAYMENT_URL/actuator/prometheus" || true)
if echo "$metrics" | grep -q "^payments"; then
  pass "payment-service prometheus output contains 'payments' metric"
else
  fail "payment-service prometheus output missing 'payments' metric"
fi

status=$(curl -s -o /dev/null -w "%{http_code}" "$NOTIFICATION_URL/actuator/prometheus")
if [[ "$status" == "200" ]]; then
  pass "notification-service /actuator/prometheus → 200"
else
  fail "notification-service /actuator/prometheus → $status (expected 200)"
fi

# ── Kafka topic reachability (via Kafdrop) ────────────────────────────────────
echo ""
echo "--- Kafka (Kafdrop UI) ---"

KAFDROP_URL="${KAFDROP_URL:-http://localhost:${PORT_KAFDROP}}"
status=$(curl -s -o /dev/null -w "%{http_code}" "$KAFDROP_URL")
if [[ "$status" == "200" ]]; then
  pass "Kafdrop UI → 200"
else
  fail "Kafdrop UI → $status (expected 200; is docker compose infra running?)"
fi

# ── Summary ────────────────────────────────────────────────────────────────────
echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
if [[ "$FAIL" -gt 0 ]]; then
  exit 1
fi
