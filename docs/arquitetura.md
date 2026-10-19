# Arquitetura

## Visão geral

**Monólito modular em camadas**, orientado por DDD. Um único deployable Spring Boot expõe APIs REST e
persiste em PostgreSQL. Os bounded contexts são separados por pacotes dentro de cada camada.

```
Cliente (app) ──► /api/public/**  ─┐
Atendente/Admin ─► /api/admin/** ──┼─► Spring Security (JWT) ─► Controllers (api)
                   /api/auth/login ┘                               │
                                                                   ▼
                                                  Casos de uso (application, @Transactional)
                                                                   │
                                                                   ▼
                                                  Modelo de domínio (domain)
                                                                   ▲
                                         Spring Data JPA / Flyway (infrastructure) ──► PostgreSQL
```

## Camadas

| Camada | Pacote | Responsabilidade | Depende de |
|---|---|---|---|
| Apresentação | `com.aruw.oficina.api` | Controllers REST, DTOs de entrada/saída, Bean Validation (`@CpfOuCnpj`, `@PlacaValida`), OpenAPI, tradução de erros (RFC 7807) | application |
| Aplicação | `com.aruw.oficina.application` | Casos de uso, **fronteira transacional**, orquestração entre agregados, publicação de eventos, modelos de leitura | domain |
| Domínio | `com.aruw.oficina.domain` | Agregados, entidades, objetos de valor, eventos, regras/invariantes, **interfaces** de repositório | — |
| Infraestrutura | `com.aruw.oficina.infrastructure` | Adaptadores Spring Data JPA, segurança JWT, usuários, listeners de notificação, configuração | domain |

## Estrutura de diretórios

```
app/src/main/java/com/aruw/oficina
├── api/                      # REST (controllers + DTOs + validação + handler de erros)
│   ├── auth/  cliente/  veiculo/  servico/  peca/  ordemservico/  publico/  validation/
├── application/              # casos de uso
│   ├── cliente/  veiculo/  catalogo/  estoque/  ordemservico/  shared/
├── domain/                   # modelo de domínio (sem dependência de Spring)
│   ├── shared/               # AggregateRoot, DomainEvent, DomainException, Documento, Placa
│   ├── cliente/  veiculo/  catalogo/
│   ├── estoque/              # Peca, MovimentacaoEstoque, eventos
│   └── ordemservico/         # OrdemServico, itens, Orcamento, Status, eventos
└── infrastructure/
    ├── persistence/          # Jpa*Repository (implementam as portas do domínio)
    ├── security/             # SecurityConfig, JWT, Usuario, AutenticacaoService
    ├── notificacao/          # NotificacaoListener (eventos após commit)
    └── config/               # OpenAPI
app/src/main/resources/db/migration   # scripts Flyway
```

## Tratamento de transações

- Cada método público dos serviços de aplicação é uma transação (`@Transactional`); consultas usam `readOnly = true`.
- **Aprovação do orçamento**: muda o status da OS, baixa o estoque de cada peça e grava as movimentações na
  **mesma transação**. Se qualquer peça não tiver saldo (`EstoqueInsuficienteException`) ou houver conflito de
  versão (`@Version`), tudo é revertido e a OS continua *Aguardando aprovação* (HTTP 409).
- **Abertura da OS**: cadastro implícito de cliente/veículo + OS + itens também são atômicos.
- Eventos de domínio são publicados após o `save` e tratados com `@TransactionalEventListener` (fase
  `AFTER_COMMIT`): notificações só saem se a transação foi confirmada.
- `spring.jpa.open-in-view=false`: os modelos de leitura (`OrdemServicoDetalhe`) são montados dentro da transação.

## Segurança

- **JWT HS256** emitido em `POST /api/auth/login` (Spring Security OAuth2 Resource Server + Nimbus).
- `/api/admin/**` exige token com perfil `ADMIN` ou `ATENDENTE`; `DELETE` exige `ADMIN`.
- `/api/public/**` (cliente) não usa token, mas exige número da OS **e** CPF/CNPJ do titular; documento
  divergente responde 404 para não revelar a existência da OS.
- Senhas com **BCrypt**; usuário inicial criado a partir de variáveis de ambiente (nada fixo no código).
- Segredo JWT obrigatório com ≥ 32 caracteres via `JWT_SECRET` (a aplicação não sobe sem ele).
- Sessão *stateless*, CSRF desabilitado (API sem cookies), cabeçalhos de segurança padrão + CSP.
- Erros não expõem stack trace (`server.error.include-stacktrace=never` e handler RFC 7807).
- Validação de dados sensíveis na borda (Bean Validation) **e** no domínio (objetos de valor).
- Imagem Docker roda com usuário não-root.
- Pipeline com Trivy (dependências, segredos, Dockerfile, imagem) e CodeQL.

## Decisões e trade-offs

| Decisão | Motivo | Trade-off |
|---|---|---|
| Anotações JPA nas classes de domínio | Evita duplicar cada agregado em uma entidade de persistência no MVP | O domínio conhece `jakarta.persistence` (apenas anotações; nenhuma dependência de Spring) |
| Portas de repositório no domínio, implementadas por Spring Data | Casos de uso dependem de interfaces do domínio | Nomes dos métodos seguem a convenção do Spring Data |
| Referência entre agregados só por UUID | Fronteiras claras, agregados pequenos | Consultas de detalhe fazem buscas adicionais |
| Preço e nome congelados nos itens da OS | Orçamento não muda se o catálogo mudar | Dados duplicados intencionalmente |
| Baixa de estoque na aprovação (não na inclusão) | Peça só sai do estoque quando o cliente autoriza | Inclusão verifica saldo, mas não reserva |
| Rejeição ⇒ Finalizada | Mantém os 6 status exigidos; veículo fica pronto para retirada | Não há status "Cancelada" separado (o orçamento fica com situação `REJEITADO`) |
