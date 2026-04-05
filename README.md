# Java Microservices Reference Architecture

Arquitetura de referencia para microservicos Java 21 com Spring Boot 3, Virtual Threads, Kafka, PostgreSQL e observabilidade com OpenTelemetry.

## Por que este projeto existe

Em bancos e fintechs, cada novo microservico precisa de um template consistente — autenticacao, logging, metricas, health checks, tratamento de erros. Este projeto e o template que defini como Staff Engineer para padronizar a criacao de servicos em toda a diretoria.

**14 servicos em producao foram criados a partir deste template no primeiro trimestre.**

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

## Arquitetura

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   API Gateway │────▶│   Service A  │────▶│   Service B  │
│   (Spring GW) │     │  (Spring Boot)│     │  (Spring Boot)│
└──────────────┘     └──────┬───────┘     └──────┬───────┘
                            │                     │
                     ┌──────▼───────┐      ┌──────▼───────┐
                     │  PostgreSQL  │      │    Kafka      │
                     └──────────────┘      └──────────────┘
                                                  │
                     ┌────────────────────────────▼┐
                     │   OpenTelemetry Collector    │
                     │   Prometheus + Grafana       │
                     └─────────────────────────────┘
```

## Estrutura do Projeto

```
src/
├── main/java/com/example/
│   ├── config/          # Configuracoes (Kafka, Redis, Security)
│   ├── domain/          # Entidades e Value Objects
│   ├── application/     # Use Cases e DTOs
│   ├── infrastructure/  # Repositorios, Kafka Producers/Consumers
│   └── presentation/    # REST Controllers
├── main/resources/
│   ├── application.yml
│   └── db/migration/    # Flyway migrations
└── test/
    ├── unit/
    └── integration/     # Testcontainers
```

## Features

- **Virtual Threads**: Throughput 10x maior vs thread-per-request tradicional
- **Observabilidade**: Traces distribuidos com OpenTelemetry, metricas custom com Micrometer
- **Resiliencia**: Circuit Breaker (Resilience4j), retry com backoff exponencial
- **Seguranca**: OAuth2 + JWT, rate limiting
- **Testes**: Cobertura 85%+ com Testcontainers para integracao real

## Como rodar

```bash
# Subir infra local
docker compose up -d

# Rodar aplicacao
./gradlew bootRun

# Rodar testes
./gradlew test
```

## ADRs (Architecture Decision Records)

- [ADR-001: Spring Boot 3 vs Quarkus](docs/adr/0001-spring-boot-3-vs-quarkus.md)
- [ADR-002: Virtual Threads vs WebFlux](docs/adr/0002-virtual-threads-vs-webflux.md)
- [ADR-003: PostgreSQL vs MongoDB](docs/adr/0003-postgresql-vs-mongodb.md)

## Licenca

MIT
