# Relatório de análise de vulnerabilidades

- **Data da análise:** 16/10/2026
- **Ferramenta:** [Trivy](https://trivy.dev) 0.57.1 (dependências, segredos, más configurações e imagem de contêiner)
- **Alvos:** repositório (`app/pom.xml`, `Dockerfile`, `docker-compose.yml`, workflows) e imagem `aruw/oficina-api:latest` (Alpine 3.24 + JRE 21)
- **Severidades consideradas:** MEDIUM, HIGH, CRITICAL
- **Saídas brutas:** [`trivy-fs.txt`](trivy-fs.txt) · [`trivy-image.txt`](trivy-image.txt)
- **Automação:** `.github/workflows/develop.yml` repete a análise a cada merge na `develop`, publica o resultado
  no GitHub Code Scanning (SARIF) e gera um **relatório em Markdown atualizado** (`relatorio-vulnerabilidades.md`, também
  exibido no resumo da execução) no artefato `vulnerabilidades`; o CodeQL (`security-and-quality`) e o PMD fazem a
  análise estática do código Java (artefato `analise-estatica`).

## Como reproduzir

```bash
docker compose build
docker run --rm -v "$PWD:/src" aquasec/trivy:0.57.1 fs --scanners vuln,secret,misconfig --skip-dirs /src/app/target /src
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy:0.57.1 image aruw/oficina-api:latest
```

## Resultado

### 1ª execução (Spring Boot 3.5.7, imagem sem patches)

| Alvo | CRITICAL | HIGH | MEDIUM |
|---|---|---|---|
| `app/pom.xml` | 0 | 4 | 0 |
| Imagem — pacotes Alpine | 0 | 0 | 2 |
| Imagem — bibliotecas Java (`app.jar`) | 9 | 25 | 27 |

### Correções aplicadas

| Ação | Onde |
|---|---|
| Spring Boot 3.5.7 → **3.5.16** (Spring Framework 6.2.19, Spring Security 6.5.x, Spring Data 3.5.x atualizados) | `app/pom.xml` |
| PostgreSQL JDBC → **42.7.14** | `app/pom.xml` (`postgresql.version`) |
| Jackson → **2.21.7** | `app/pom.xml` (`jackson-bom.version`) |
| Tomcat embarcado → **10.1.61** | `app/pom.xml` (`tomcat.version`) |
| Log4j API → **2.25.5**; commons-lang3 → **3.18.0** | `app/pom.xml` |
| springdoc-openapi → **2.8.17** | `app/pom.xml` |
| `apk upgrade` na imagem de runtime (libpng, zlib) | `app/Dockerfile` |

Após as correções, os 100 testes automatizados continuaram passando e a cobertura mínima foi mantida.

### Execução final

| Alvo | CRITICAL | HIGH | MEDIUM |
|---|---|---|---|
| Repositório (dependências, segredos, IaC) | 0 | 0 | 0 |
| Imagem — pacotes Alpine | 0 | 0 | 0 |
| Imagem — bibliotecas Java (`app.jar`) | 2 | 0 | 0 |

### Vulnerabilidades remanescentes — análise de aplicabilidade

| CVE | Componente | Correção disponível | Aplicável? | Justificativa |
|---|---|---|---|---|
| CVE-2026-47884 | `spring-webmvc` 6.2.19 | Só no Spring Framework 7.0.9 (Spring Boot 4) | **Não** | Exige uso de `XsltView` com mapeamento `/**` que renderize *views*. A API usa apenas `@RestController` com respostas JSON; não há `XsltView`, *view resolvers* ou templates. |
| CVE-2026-47890 | `spring-webmvc` 6.2.19 | Só no Spring Framework 7.0.9 | **Não** | Afeta Server-Sent Events com *view fragments*. A aplicação não expõe SSE nem fragments. |

**Risco residual:** baixo. As duas CVEs dependem de recursos que a aplicação não utiliza. A mitigação definitiva
é migrar para Spring Boot 4.x (Spring Framework 7), planejada como evolução pós-MVP; o job de vulnerabilidades do workflow da `develop`
sinaliza quando houver versão corrigida disponível.

## Controles de segurança da aplicação

| Controle | Implementação |
|---|---|
| Autenticação das APIs administrativas | JWT HS256 (`/api/auth/login`), validação de assinatura, expiração e emissor |
| Autorização | Perfis `ADMIN` / `ATENDENTE`; `DELETE` restrito a `ADMIN` |
| Acesso do cliente | OS + CPF/CNPJ do titular; documento divergente ⇒ 404 (não revela existência) |
| Senhas | BCrypt; usuário inicial via variáveis de ambiente |
| Segredos | Nenhum segredo no código; `JWT_SECRET` obrigatório (≥ 32 caracteres); `.env` no `.gitignore` |
| Validação de entrada | Bean Validation + objetos de valor (`Documento`, `Placa`) |
| Erros | RFC 7807 sem stack trace |
| Integridade de dados | Transações, `@Version` (lock otimista), `UNIQUE`/`CHECK`/FKs no banco |
| Contêiner | Usuário não-root, imagem JRE Alpine com patches, healthcheck |
| Cadeia de suprimentos | Trivy + CodeQL no GitHub Actions |

## Análise estática (CodeQL) — alertas tratados

| Alerta | Severidade | Tratamento |
|---|---|---|
| `java/spring-disabled-csrf-protection` — CSRF desabilitado em `SecurityConfig` | High | **Decisão intencional, documentada no código.** CSRF explora credenciais que o navegador envia sozinho (cookies de sessão). A API é *stateless* (`SessionCreationPolicy.STATELESS`), não emite cookies e autentica apenas pelo cabeçalho `Authorization: Bearer`, que o navegador não anexa automaticamente a requisições de outros sites. As rotas públicas do cliente exigem CPF/CNPJ no corpo/parâmetro. Seguindo a recomendação do Spring Security para APIs consumidas por clientes não baseados em sessão, o alerta é dispensado como *won't fix* no GitHub Code Scanning; no SonarQube Cloud (regra `java:S4502`) a linha é marcada com `// NOSONAR` e o motivo. |
| `java/unused-parameter` — parâmetros não usados em `ApiExceptionHandler` | Note | **Corrigido:** os handlers de `OptimisticLockingFailureException` e de requisição mal formada não recebem mais a exceção, pois não a utilizam (o tipo continua declarado em `@ExceptionHandler`). |

## Qualidade de código (SonarQube Cloud)

Análise contínua a cada merge na `develop` (job *Análise estática* do workflow 🔨 Develop), com a cobertura do JaCoCo.
Painel: <https://sonarcloud.io/summary/overall?id=Aruw_Oficina-Mecanica&branch=develop>

| Métrica | Resultado na `develop` |
|---|---|
| Quality Gate | Aprovado |
| Bugs | 0 |
| Vulnerabilidades | 0 |
| Security hotspots | 0 |
| Duplicação | 0,0% |
| Cobertura (projeto inteiro) | 95,5% |

Os 36 apontamentos da primeira análise (11 críticos de chamada transacional via `this`, fuso horário em `Year.now()`,
asserções ambíguas nos testes, entre outros) foram corrigidos.

