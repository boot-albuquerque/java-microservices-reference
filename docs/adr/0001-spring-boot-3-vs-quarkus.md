# ADR-001: Spring Boot 3 vs Quarkus

## Status: Aceito

## Contexto
Precisavamos escolher o framework para os novos microservicos Java 21.

## Opcoes Consideradas
1. **Spring Boot 3** — ecossistema maduro, grande comunidade, compatibilidade com libs existentes
2. **Quarkus** — startup rapido, menor consumo de memoria, compilacao nativa (GraalVM)

## Decisao
Escolhemos **Spring Boot 3** porque:
- 90% do time ja tem experiencia com Spring
- Ecossistema de libs (Spring Security, Spring Data, Spring Cloud) e mais maduro
- Virtual Threads (Java 21) eliminam a vantagem de performance reativa do Quarkus
- Compatibilidade com servicos legados existentes

## Consequencias
- Startup mais lento que Quarkus (~2s vs ~0.5s), aceitavel para servicos long-running
- Maior consumo de memoria base (~200MB vs ~50MB), compensado por Virtual Threads
