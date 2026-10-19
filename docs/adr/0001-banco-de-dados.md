# ADR 0001 — Banco de dados: PostgreSQL

- **Status:** aceito
- **Contexto:** MVP do back-end da oficina (Fase 1)

## Contexto

O núcleo do sistema é a **Ordem de Serviço**, que reúne cliente, veículo, itens de serviço, peças/insumos,
orçamento e histórico de status. Os fluxos principais exigem **consistência forte**:

- aprovar um orçamento precisa, **na mesma operação**, mudar o status da OS, baixar o estoque de vários
  itens e registrar as movimentações — ou nada disso pode acontecer;
- o saldo de estoque **nunca** pode ficar negativo, mesmo com duas OS sendo aprovadas ao mesmo tempo;
- CPF/CNPJ e placa precisam ser **únicos**;
- a gestão precisa de consultas agregadas (listagens por status, tempo médio de execução).

## Decisão

Usar **PostgreSQL 16** como banco relacional, com:

- **Transações ACID** controladas por `@Transactional` nos casos de uso da camada `application`;
- **Restrições no banco** como segunda linha de defesa: `UNIQUE` (documento, placa, código da peça),
  `CHECK (quantidade_estoque >= 0)` e chaves estrangeiras;
- **Lock otimista** (`@Version`) nos agregados para evitar baixa dupla de estoque em aprovações concorrentes;
- **Flyway** para versionar o esquema (`app/src/main/resources/db/migration`);
- **H2 em modo PostgreSQL** apenas nos testes automatizados, executando as mesmas migrações.

## Alternativas consideradas

| Opção | Por que não |
|---|---|
| MongoDB (documento) | A OS caberia bem em um documento, mas a baixa de estoque envolve **vários agregados** (OS + N peças). Transações multi-documento existem, porém são menos naturais e exigem replica set; unicidade e integridade referencial ficariam na aplicação. |
| MySQL | Atenderia, mas o PostgreSQL oferece `CHECK` constraints confiáveis, tipo `UUID` nativo, `TIMESTAMP WITH TIME ZONE` e melhor suporte a consultas analíticas para métricas futuras. |
| H2/SQLite em produção | Não adequados para ambiente multiusuário nem para execução em contêiner com persistência real. |

## Consequências

- ✅ Integridade garantida no banco e na aplicação; rollback automático em qualquer falha do fluxo.
- ✅ Esquema evolutivo e reprodutível (Flyway) — o mesmo script roda local, no Docker e nos testes.
- ✅ Imagem oficial `postgres:16-alpine` simplifica o `docker-compose`.
- ⚠️ Mapeamento objeto-relacional (JPA) adiciona anotações ao modelo de domínio — trade-off aceito no MVP
  (ver [arquitetura](../arquitetura.md#decisões-e-trade-offs)).
