# Oficina Mecânica — Sistema Integrado de Atendimento e Execução de Serviços (MVP)

Back-end da Fase 1 do curso **SOAT — Software Architecture (FIAP)**.

> 📹 **Vídeo da entrega:** `<COLE AQUI O LINK DO YOUTUBE>`
> 🧭 **Documentação DDD (Miro):** `<COLE AQUI O LINK DO MIRO>` · cópia no repositório: [docs/ddd](docs/ddd/README.md)

## Identificação

| Curso | Fase | Integrante | RM | Discord |
|---|---|---|---|---|
| SOAT — Software Architecture | 1 | André Wada Gromann | RM377153 | wadaruw |

**Linguagens:** Java 21 e SQL (PostgreSQL) · **Principais tecnologias:** Spring Boot 3.5, Spring Security (JWT),
Spring Data JPA, PostgreSQL 16, Flyway, Swagger/OpenAPI, JUnit 5, Mockito, JaCoCo, Docker e GitHub Actions.

## Objetivo

Substituir anotações manuais e planilhas de uma oficina mecânica de médio porte por um sistema que organize
o atendimento, o diagnóstico, o orçamento, a execução e a entrega dos veículos, permitindo que o cliente
acompanhe a Ordem de Serviço e aprove o orçamento pela API.

## Solução

API REST monolítica que cobre:

- **Ordem de Serviço (OS):** abertura com identificação do cliente por **CPF/CNPJ** e do veículo pela **placa**
  (cadastrados no ato se ainda não existirem), inclusão de **serviços, peças e insumos**, **orçamento automático**,
  envio ao cliente, **aprovação/rejeição** e ciclo de status
  *Recebida → Em diagnóstico → Aguardando aprovação → Em execução → Finalizada → Entregue*, com histórico.
- **Acompanhamento pelo cliente:** `/api/public/**` — consulta e decisão do orçamento com o número da OS + CPF/CNPJ.
- **Gestão administrativa:** CRUD de clientes, veículos, serviços e peças/insumos; **controle de estoque**
  (entradas, baixa automática na aprovação, histórico, alerta de estoque mínimo); listagem da fila de OS por
  prioridade; **tempo médio de execução** geral e por serviço.
- **Segurança e qualidade:** JWT nas APIs administrativas, validação de CPF/CNPJ (inclusive **CNPJ alfanumérico**)
  e placa (antiga e Mercosul), 100 testes automatizados, cobertura ≥ 80% verificada no build, análise de vulnerabilidades.

## Arquitetura

Monólito em camadas guiado por **Domain-Driven Design** — detalhes em [docs/arquitetura.md](docs/arquitetura.md).

| Camada | Pacote (`com.aruw.oficina`) | Papel |
|---|---|---|
| Apresentação | `api` | Controllers REST, DTOs, validação, OpenAPI, erros RFC 7807 |
| Aplicação | `application` | Casos de uso transacionais, publicação de eventos |
| Domínio | `domain` | Agregados, objetos de valor, eventos, regras de negócio |
| Infraestrutura | `infrastructure` | JPA, segurança JWT, notificações, configuração |

Bounded contexts: **Ordem de Serviço** (core), **Estoque**, **Catálogo**, **Atendimento** (clientes/veículos)
e **Identidade e Acesso**.

## Tecnologias

Java 21 · Spring Boot 3.5 (Web, Data JPA, Validation, Security, OAuth2 Resource Server/JWT, Actuator) ·
PostgreSQL 16 · Flyway · springdoc-openapi (Swagger UI) · JUnit 5, Mockito, AssertJ, MockMvc, H2 · JaCoCo ·
Docker / Docker Compose · GitHub Actions (CI, CodeQL, PMD, Trivy, Release com GHCR).

## Banco de dados — decisão

**PostgreSQL**, porque os fluxos centrais exigem **transações ACID** envolvendo vários agregados (aprovar
orçamento = mudar status da OS + baixar estoque de várias peças + registrar movimentações, tudo ou nada),
**restrições de integridade** (CPF/CNPJ, placa e código únicos; estoque nunca negativo via `CHECK`) e
**lock otimista** contra baixa dupla. O esquema é versionado com Flyway.
Alternativas avaliadas e consequências: [docs/adr/0001-banco-de-dados.md](docs/adr/0001-banco-de-dados.md).

## Pré-requisitos

- **Docker** e **Docker Compose** (para subir tudo), **ou**
- **JDK 21+** e **Maven 3.9+** (para rodar/testar sem Docker; os testes usam H2 em memória).

## Execução

### Com Docker Compose (recomendado)

```bash
cp .env.example .env
docker compose up -d --build
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

Para parar: `docker compose down` (adicione `-v` para apagar o banco).

### Localmente (sem Docker para a API)

```bash
docker compose up -d db
cd app
DB_URL=jdbc:postgresql://localhost:5432/oficina DB_USERNAME=oficina DB_PASSWORD=oficina-local \
JWT_SECRET=troque-este-segredo-local-com-32-caracteres-ou-mais \
ADMIN_USERNAME=admin ADMIN_PASSWORD=admin123 \
mvn spring-boot:run
```

| Variável | Descrição |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexão com o PostgreSQL |
| `JWT_SECRET` | Segredo HS256 (obrigatório, ≥ 32 caracteres) |
| `JWT_EXPIRATION` | Validade do token (ISO-8601, padrão `PT2H`) |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Usuário administrador criado na primeira inicialização |

## Testes

```bash
cd app
mvn verify
```

Executa **testes unitários** (domínio e casos de uso) e **testes de integração** (aplicação completa + Flyway + H2,
chamadas HTTP via MockMvc) e valida a **cobertura mínima de 80%** dos domínios críticos (`domain` + `application`)
com JaCoCo — o build falha abaixo disso. Relatório: `app/target/site/jacoco/index.html`.

| Escopo | Linhas | Instruções |
|---|---|---|
| `domain` | 100% | 99,5% |
| `application` | 100% | 98,9% |
| **Domínios críticos (total)** | **100%** | **99,2%** |

Testes de integração dos fluxos principais: `AutenticacaoIT`, `CadastrosIT`, `OrdemServicoFluxoIT`.

## Documentação da API

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Collection do Postman: [docs/api/oficina.postman_collection.json](docs/api/oficina.postman_collection.json)
  — importe no Postman, execute **0. Autenticação → Login (admin)** (o token é salvo automaticamente) e rode a pasta
  **1. Fluxo completo da OS** em ordem ou no Collection Runner. Cada requisição tem testes de status/regra.
  Também roda via linha de comando:
  `docker run --rm -v "$PWD/docs/api:/etc/newman" postman/newman:6-alpine run oficina.postman_collection.json --env-var baseUrl=http://host.docker.internal:8080`
- Roteiro de chamadas para demonstração (VS Code / IntelliJ): [docs/api/oficina.http](docs/api/oficina.http)

Principais rotas:

| Método | Rota | Descrição |
|---|---|---|
| POST | `/api/auth/login` | Obtém o token JWT |
| CRUD | `/api/admin/clientes`, `/veiculos`, `/servicos`, `/pecas` | Gestão administrativa |
| POST | `/api/admin/pecas/{id}/entradas` | Entrada de estoque |
| POST | `/api/admin/ordens-servico` | Abre OS |
| GET | `/api/admin/ordens-servico[?status=]` | Fila de OS por prioridade |
| POST | `/api/admin/ordens-servico/{id}/servicos` · `/pecas` | Inclui itens |
| POST | `/api/admin/ordens-servico/{id}/diagnostico` · `/orcamento` · `/orcamento/aprovacao` · `/orcamento/rejeicao` · `/finalizacao` · `/entrega` | Ciclo de vida |
| GET | `/api/admin/ordens-servico/metricas/tempo-medio-execucao` | Tempo médio de execução |
| GET | `/api/public/ordens-servico/{id}?documento=` | Cliente acompanha a OS |
| POST | `/api/public/ordens-servico/{id}/orcamento/aprovacao` · `/rejeicao` | Cliente decide o orçamento |

## Autenticação e credenciais de demonstração

O usuário administrador é criado na inicialização a partir de `ADMIN_USERNAME`/`ADMIN_PASSWORD`.
Com os valores de demonstração do `docker-compose.yml`/`.env.example`:

| Usuário | Senha | Perfil |
|---|---|---|
| `admin` | `admin123` | ADMIN |

> Credenciais apenas para demonstração local. Em qualquer outro ambiente, defina valores próprios no `.env`.

1. `POST /api/auth/login` com `{"username":"admin","password":"admin123"}`;
2. use o `accessToken` no cabeçalho `Authorization: Bearer <token>` (no Swagger: botão **Authorize**).

As rotas `/api/public/**` não usam token: o cliente informa o número da OS e o próprio CPF/CNPJ.

Dados de demonstração carregados pelo Flyway: serviços *Troca de óleo*, *Alinhamento*, *Balanceamento*,
*Revisão de freios* e itens de estoque *Óleo 5W30*, *Filtro de óleo*, *Pastilhas de freio*, *Fluido de freio*.

## Estrutura do repositório

```
.
├── app/                               # aplicação Spring Boot
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/aruw/oficina/
│       │   ├── api/                   # controllers REST, DTOs, validação
│       │   ├── application/           # casos de uso
│       │   ├── domain/                # modelo de domínio (DDD)
│       │   └── infrastructure/        # JPA, segurança, notificações
│       ├── main/resources/db/migration/   # Flyway
│       └── test/                      # testes unitários e de integração
├── docs/
│   ├── ddd/                           # event storming, diagramas, linguagem ubíqua (md + pdf)
│   ├── adr/                           # decisões de arquitetura
│   ├── seguranca/                     # relatório de vulnerabilidades
│   ├── processo/                      # fluxo de PR e proteção da main
│   ├── api/                           # roteiro de chamadas .http
│   ├── entrega/                       # roteiro do PDF e do vídeo
│   └── arquitetura.md
├── .github/                           # workflows (validate, develop, release)
├── docker-compose.yml
└── .env.example
```

## Documentação

- DDD (Event Storming, diagramas, linguagem ubíqua, relação com o código): [docs/ddd](docs/ddd/README.md)
- Arquitetura: [docs/arquitetura.md](docs/arquitetura.md)
- Decisão do banco: [docs/adr/0001-banco-de-dados.md](docs/adr/0001-banco-de-dados.md)
- Análise de vulnerabilidades: [docs/seguranca/relatorio-vulnerabilidades.md](docs/seguranca/relatorio-vulnerabilidades.md)
- CI/CD, versionamento (`-SNAPSHOT`), releases e proteção de branches: [docs/processo/github.md](docs/processo/github.md)
  - `validate.yml`: push em qualquer branch → build, testes e cobertura ≥ 80%
  - `develop.yml`: merge na `develop` → testes, análise estática (CodeQL/PMD) e vulnerabilidades (Trivy), com artefatos
  - `release.yml`: manual a partir da `main` → tag, GitHub Release, imagem no GHCR e próxima versão `X.Y.(Z+1)-SNAPSHOT`

## Vídeo

`<COLE AQUI O LINK DO YOUTUBE>`
