# Java Microservices Reference Architecture

[![CI](https://github.com/LucasGeek/java-microservices-reference/actions/workflows/ci.yml/badge.svg)](https://github.com/LucasGeek/java-microservices-reference/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Arquitetura de referencia para microservicos Java 21 com Spring Boot 3, Virtual Threads, Kafka, PostgreSQL e observabilidade com OpenTelemetry.

## Por que este projeto existe

Em bancos e fintechs, cada novo microservico precisa de um template consistente — autenticacao, logging, metricas, health checks, tratamento de erros. Este repositorio condensa o padrao arquitetural que usei em ambientes de producao de grande porte, entregando um blueprint reaproveitavel para novos servicos.

---

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Runtime | Java 21 (Virtual Threads / Project Loom) |
| Framework | Spring Boot 3.3 + Spring Cloud |
| Mensageria | Apache Kafka (Spring Kafka) |
| Banco de Dados | PostgreSQL 16 + Flyway |
| Cache | Redis |
| Observabilidade | OpenTelemetry + Prometheus + Grafana |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Build | Gradle 8 + Docker multi-stage |
| Deploy | Kubernetes (Helm) + ArgoCD |

---

## Quick Start

### Pre-requisitos

- Java 21+
- Docker + Docker Compose
- `openssl` (para o smoke-test; presente por padrao no macOS e Linux)

### 1. Configurar variaveis de ambiente

```bash
cp .env.example .env
# Editar .env se necessario (o default funciona para desenvolvimento local)
```

### 2. Subir infraestrutura

```bash
docker compose up -d
```

Aguarde ~60s ate todos os servicos estarem `healthy`:

```bash
docker compose ps
```

### 3. Build e execucao local

```bash
# Compilar todos os modulos
./gradlew build

# Executar testes unitarios
./gradlew test

# Executar testes de integracao (requer Docker)
./gradlew integrationTest

# Formatar codigo
./gradlew spotlessApply
```

### 4. Smoke test

```bash
chmod +x scripts/smoke-test.sh
./scripts/smoke-test.sh
```

O script gera JWTs via `openssl` (sem dependencias externas), executa health checks, POST/GET, assercoes de 401/400 e idempotencia, e valida metricas Prometheus.

---

## URLs locais

As portas host sao configuráveis via `.env` (veja `.env.example`). Os defaults abaixo usam o prefixo `1` para evitar conflitos.

| Service | URL (default) | Variavel `.env` | Descricao |
|---------|---------------|-----------------|-----------|
| Payment API (Swagger) | http://localhost:${PORT_PAYMENT:-18081}/swagger-ui.html | `PORT_PAYMENT` | Documentacao interativa |
| Payment Health | http://localhost:${PORT_PAYMENT:-18081}/actuator/health | `PORT_PAYMENT` | Health check |
| Payment Metrics | http://localhost:${PORT_PAYMENT:-18081}/actuator/prometheus | `PORT_PAYMENT` | Metricas Prometheus |
| Notification Health | http://localhost:${PORT_NOTIFICATION:-18082}/actuator/health | `PORT_NOTIFICATION` | Health check |
| Notification Metrics | http://localhost:${PORT_NOTIFICATION:-18082}/actuator/prometheus | `PORT_NOTIFICATION` | Metricas Prometheus |
| Grafana | http://localhost:${PORT_GRAFANA:-13000} | `PORT_GRAFANA` | Dashboards (admin/admin) |
| Prometheus | http://localhost:${PORT_PROMETHEUS:-19090} | `PORT_PROMETHEUS` | Query de metricas |
| Kafdrop (Kafka UI) | http://localhost:${PORT_KAFDROP:-19000} | `PORT_KAFDROP` | Inspecao de topicos |

### Troubleshooting: Port conflicts

Se alguma porta já estiver em uso na sua máquina, sobrescreva no `.env`:

```bash
# Exemplo: outro serviço já usa 18081
PORT_PAYMENT=28081
```

Portas que podem conflitar com ferramentas comuns:

| Variavel | Default | Conflito comum |
|----------|---------|----------------|
| `POSTGRES_PORT` | `15432` | PostgreSQL local na `5432` |
| `PORT_REDIS` | `16379` | Redis local na `6379` |
| `PORT_KAFKA` | `19092` | Kafka local na `9092` |
| `PORT_GRAFANA` | `13000` | Grafana local na `3000` |
| `PORT_PROMETHEUS` | `19090` | Prometheus local na `9090` |

Para verificar portas em uso: `lsof -i :<porta>` (macOS/Linux)

---

## Arquitetura

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────────┐
│   API Client │────▶│  payment-service │────▶│  notification-service│
│              │     │  :8081           │     │  :8082               │
└──────────────┘     └────────┬─────────┘     └──────────┬───────────┘
                              │                           │
                     ┌────────▼─────────┐     ┌──────────▼───────────┐
                     │   PostgreSQL 16  │     │   Apache Kafka        │
                     │   + Flyway       │     │   (transfer-events)   │
                     └──────────────────┘     └──────────────────────┘
                                                          │
                     ┌────────────────────────────────────▼┐
                     │   OpenTelemetry Collector            │
                     │   Prometheus :9090 + Grafana :4000   │
                     └──────────────────────────────────────┘
```

### Estrutura dos modulos

```
java-microservices-reference/
├── payment-service/          # REST API: payments (Clean Architecture)
│   └── src/
│       ├── main/java/com/example/payment/
│       │   ├── domain/       # Model, StateMachine, domain events, ports
│       │   ├── application/  # Use cases, DTOs, application ports
│       │   ├── infrastructure/ # JPA, Kafka producer, Redis, Resilience4j
│       │   └── presentation/ # REST controllers, JWT filter, exception handler
│       ├── test/             # Unit tests (JUnit 5 + Mockito)
│       └── integrationTest/  # Testcontainers (Postgres, Redis, Kafka)
├── notification-service/     # Kafka consumer + DLQ + structured JSON log + metric
├── charts/                   # Helm charts (payment-service, notification-service)
├── observability/            # otel-collector, Prometheus, Grafana dashboard
├── docs/
│   ├── adr/                  # Architecture Decision Records
│   └── acceptance-criteria-checklist.md
└── scripts/
    └── smoke-test.sh         # Validacao end-to-end pos-deploy
```

---

## Features

- **Virtual Threads**: Throughput elevado sem complexidade reativa (Project Loom)
- **Clean Architecture**: domain com zero dependencias de framework (portas + adapters)
- **Idempotencia**: `Idempotency-Key` header + Redis SETNX para deduplicacao (race-safe)
- **Rate limiting**: Redis fixed-window (100 req/min por `userId`) via INCR + EXPIRE
- **Circuit Breaker + Retry**: Resilience4j no producer Kafka (fallback absorve falha, nao propaga)
- **Observabilidade**: OpenTelemetry Java agent (traces + metrics), Micrometer counters custom
- **Seguranca**: JWT HS256 via jjwt (production should use OAuth2/OIDC — ver Known Limitations)
- **DLQ**: `payment.events.DLT` com `ErrorHandlingDeserializer` (preserva payload bruto no header)
- **Testes**: Cobertura agregada >=85% (unit + Testcontainers: Postgres, Redis, Kafka)
- **Outbox pattern**: documentado no [ADR-005](docs/adr/0005-transactional-outbox-pattern.md) como upgrade de producao (nao implementado neste template)

---

## CI/CD

O pipeline GitHub Actions (`.github/workflows/ci.yml`) executa 5 jobs:

| Job | Trigger | Descricao |
|-----|---------|-----------|
| `test` | todo PR/push | Unit + integration tests (Testcontainers) |
| `build` | apos `test` | Compila JARs via Gradle |
| `security` | apos `build` | Scan de vulnerabilidades com Trivy (HIGH/CRITICAL) |
| `publish` | push em `main` | Publica imagens no GHCR |
| `helm-lint` | apos `test` | Valida os Helm charts |

### Deploy com Helm

```bash
# Instalar payment-service
helm install payment charts/payment-service \
  --set image.tag=<sha> \
  --set config.springDatasourceUrl=jdbc:postgresql://postgres:5432/payments \
  --set secrets.jwtSecret=<your-secret-min-32-chars>

# Instalar notification-service
helm install notification charts/notification-service \
  --set image.tag=<sha> \
  --set config.kafkaBootstrapServers=kafka:9092
```

---

## Architecture Decision Records (ADRs)

Decisoes arquiteturais documentadas em [`docs/adr/`](docs/adr/):

| ADR | Titulo |
|-----|--------|
| [ADR-001](docs/adr/0001-spring-boot-3-vs-quarkus.md) | Spring Boot 3 vs Quarkus |
| [ADR-002](docs/adr/0002-virtual-threads-vs-webflux.md) | Virtual Threads vs WebFlux |
| [ADR-003](docs/adr/0003-postgresql-vs-mongodb.md) | PostgreSQL vs MongoDB |
| [ADR-004](docs/adr/0004-kafka-vs-rabbitmq.md) | Kafka vs RabbitMQ |
| [ADR-005](docs/adr/0005-transactional-outbox-pattern.md) | Transactional Outbox Pattern (documented; not implemented — see Known Limitations) |
| [ADR-006](docs/adr/0006-clean-architecture-domain-isolation.md) | Clean Architecture + Domain Isolation |

### Acceptance Criteria
[`docs/acceptance-criteria-checklist.md`](docs/acceptance-criteria-checklist.md) — 29 criterios mapeados do deep-interview spec com evidencia (arquivo/teste/comando).

---

## Troubleshooting

### Port conflict (PostgreSQL)

Se voce ja tem PostgreSQL rodando localmente, `docker compose up` falha com `bind: address already in use` na porta 5432.

```bash
# 1. Confirmar conflito
lsof -i :5432

# 2. Mudar a porta exposta (container continua em 5432 internamente)
echo "POSTGRES_PORT=15432" >> .env
docker compose up -d
```

O default do projeto ja e `POSTGRES_PORT=15432` no `.env.example`, entao na pratica o conflito so aparece se voce sobrescreveu para `5432`.

### Mac Apple Silicon / M1-M4 Docker build

**Sintoma:** `docker compose build` ou `docker compose up --build` crasha durante `gradle :<service>:bootJar` com `SIGILL (Illegal instruction)` em `java.lang.System.registerNatives()` em `linux-aarch64`.

**Causa:** a imagem oficial `gradle:8.10-jdk21*` tem problemas conhecidos no Apple Silicon (ARM64) com JDK 21 (SIGILL ao invocar instrucoes nativas). O projeto evita isso usando **`eclipse-temurin:21-jdk-jammy` + Gradle Wrapper (`./gradlew`)** no build stage — build deterministico e estavel em ambos arm64 e amd64.

**Solucao:**

1. Garantir que `.dockerignore` esta presente na raiz (exclui `**/build/`, `.gradle/`, `.git/`, `docs/`, etc — reduz contexto de ~6GB para <10MB).
2. Usar `DOCKER_PLATFORM=linux/arm64` no `.env` (padrao). Apple Silicon roda nativo.
3. Se o SIGILL persistir, fallback para emulacao x86_64:
   ```bash
   DOCKER_PLATFORM=linux/amd64 docker compose build --no-cache payment-service notification-service
   DOCKER_PLATFORM=linux/amd64 docker compose up -d
   ```
4. Docker Desktop → Settings → Resources: aumentar memoria para 6-8GB.
5. Rodar `./gradlew clean` no host antes de buildar para garantir que `build/` local nao polui o contexto.

**Comando recomendado Mac M4:**
```bash
./gradlew clean
docker compose build --no-cache payment-service notification-service
docker compose up -d
```

### Kafdrop platform warning em Apple Silicon

**Sintoma:** `docker compose up` loga:
```
WARNING: The requested image's platform (linux/amd64) does not match the detected host platform (linux/arm64/v8)
```

**Causa:** a imagem `obsidiandynamics/kafdrop` upstream publica apenas `linux/amd64`. Em Mac M1/M2/M3/M4 ela roda via emulacao Rosetta/QEMU — funcional, so um pouco mais lento.

**Estado:** esperado e documentado. `docker-compose.infra.yml` define `platform: ${KAFDROP_PLATFORM:-linux/amd64}` explicitamente para silenciar o warning. Se no futuro surgir uma tag arm64 upstream, trocar `KAFDROP_PLATFORM=linux/arm64` no `.env`.

### Contexto Docker grande (>1GB)

`docker build` copia tudo no context dir (`.`). Se `build/` ou `.gradle/` tem GBs de artefatos de teste (Testcontainers, Jacoco exec, reports), o contexto explode.

**Solucao:** `.dockerignore` na raiz ja cobre isso. Verificar com:
```bash
du -sh .
# Esperado: <100MB se .dockerignore aplicado. Se >1GB, rodar ./gradlew clean.
```

### Testcontainers falha localmente

`./gradlew integrationTest` exige Docker daemon rodando. Se falhar:
- `docker info` deve responder sem erro
- Linux: talvez precise setar `DOCKER_HOST=unix:///var/run/docker.sock`
- macOS/Windows: abrir Docker Desktop antes

### Rate limiting sob concorrencia alta

O rate limiter usa Redis (`INCR + EXPIRE`) e esta coberto por **unit tests** + **integration tests** (cenarios sequenciais com 100 req/min → 429 correto no 101).

Porem, sob **carga concorrente alta** (100+ requests paralelos disparados ao mesmo tempo pelo mesmo `userId`), parte dos requests pode passar antes do filter aplicar o bloqueio, por conta de:

1. **INCR + EXPIRE nao-atomico** (race curta entre os dois comandos Redis)
2. **Ordem do filter chain** do Spring Security em cenarios de contencao

Em producao, a abordagem correta seria:

- **Script Lua atomico** no Redis (`EVAL`), garantindo `INCR + EXPIRE` em uma unica round-trip
- **Algoritmo de janela deslizante** (sliding window) em vez de fixed-window
- **Rate limiting no API Gateway** (Kong, Envoy, AWS API Gateway) — fora do app, antes mesmo do servico receber a requisicao

Este trade-off ja esta contemplado em [Known Limitations](#known-limitations-portfolio-trade-offs).

### JWT errors no smoke-test

Validar `.env`:
- `JWT_SECRET` deve ter ao menos 32 caracteres (HS256 requirement da jjwt)
- Recriar com: `openssl rand -hex 32` e colocar em `.env`

---

## Known Limitations (Portfolio Trade-offs)

Os itens abaixo sao simplificacoes deliberadas para um repositorio de referencia:

| Limitacao | Motivo | Producao faria diferente |
|-----------|--------|--------------------------|
| **Outbox pattern NAO implementado** | Reduz complexidade do template | Outbox transacional (relay poller) para garantir exactly-once DB↔Kafka. Ver [ADR-005](docs/adr/0005-transactional-outbox-pattern.md) |
| **`ProcessPaymentUseCase` sem `@Transactional`** | Demo mostra state machine, nao consistencia de txn | Wrapper `@Service @Transactional` ou agrupar os 2 saves em uma unica transacao |
| **Rate limiter Redis INCR + EXPIRE nao-atomico** | Trade-off de simplicidade | Script Lua atomico ou Resilience4j `RateLimiter` | 
| **JWT HS256 com segredo estatico** | Simplicidade do template | OAuth2/OIDC (Keycloak, Auth0, Cognito) com JWKS + RS256 |
| **Schema Registry sem auth** | Reduz complexidade local | TLS + SASL no Schema Registry em producao |
| **DLQ IT flaky (`PaymentEventConsumerDlqIT`)** | `EmbeddedKafka` + `mock://` schema registry tem timing nao-deterministico | Testcontainers `KafkaContainer` + `SchemaRegistryContainer` real |
| **Grafana dashboard minimo** | Demonstra integracao, nao e dashboard completo de SRE | Dashboard bem-definido por servico (RED/USE metrics, alertas) |

---

## Licenca

MIT
