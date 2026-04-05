# ADR-003: PostgreSQL vs MongoDB

## Status: Aceito

## Contexto
Escolha de banco de dados para servicos transacionais bancarios.

## Decisao
Escolhemos **PostgreSQL** porque:
- Transacoes ACID sao obrigatorias para operacoes financeiras
- Suporte nativo a JSON (jsonb) quando precisamos de flexibilidade
- Ecossistema maduro de ferramentas (pg_stat, EXPLAIN ANALYZE)
- Compliance bancario exige auditabilidade que RDBMS oferece nativamente

## Consequencias
- Schema migrations obrigatorias (Flyway)
- Sharding mais complexo que MongoDB (usar Citus se necessario)
