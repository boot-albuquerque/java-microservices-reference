# ADR-002: Virtual Threads vs WebFlux

## Status: Aceito

## Contexto
Servicos com alta concorrencia (50k+ req/min) precisam de modelo eficiente de threading.

## Opcoes Consideradas
1. **Virtual Threads (Project Loom)** — modelo imperativo com threads leves
2. **WebFlux (Reactor)** — modelo reativo com Mono/Flux

## Decisao
Escolhemos **Virtual Threads** porque:
- Codigo imperativo e mais simples de ler, debugar e manter
- Nao requer reescrita de todo o stack (drivers, libs)
- Performance equivalente ao WebFlux para I/O-bound workloads
- Time de 8 engenheiros adapta em dias vs semanas para reativo

## Consequencias
- Requer Java 21+ (nao retrocompativel)
- Algumas libs (Hibernate) precisam de versoes atualizadas
- CPU-bound workloads nao se beneficiam (usar thread pools tradicionais)
