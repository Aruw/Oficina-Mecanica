# Processo de versionamento e CI/CD (GitHub)

## Fluxo de branches

```
feature/*  ──PR──►  develop  ──PR──►  main  ──(Release manual)──►  tag vX.Y.Z + GitHub Release + imagem Docker
   │                   │                │
   validate.yml        develop.yml      —                 release.yml
```

1. Crie uma branch a partir da `develop`: `feature/<descricao>`. Cada push roda o **🔍 Validate** (build, testes e cobertura ≥ 80%).
2. Abra um **Pull Request** para `develop`.
3. Ao fazer o merge, o push na `develop` roda o pipeline **🔨 Develop** (qualidade e segurança).
4. Para entregar, abra um PR `develop → main`. Depois do merge, execute o workflow **📦 Release** manualmente.
5. A entrega final usa a tag gerada pela release (ex.: `v1.0.0`); informe a tag e o hash do commit no PDF.

## Versionamento (`-SNAPSHOT`)

| Onde | Versão no `app/pom.xml` | Exemplo |
|---|---|---|
| `develop`, `main` e branches de trabalho | versão em desenvolvimento, **com** `-SNAPSHOT` | `1.0.0-SNAPSHOT` |
| Tag / GitHub Release / imagem Docker | versão final, **sem** `-SNAPSHOT` | `1.0.0` |
| Depois da release | patch incrementado, **com** `-SNAPSHOT` (via PR automático) | `1.0.1-SNAPSHOT` |

O commit da release (pom com `1.0.0`) fica **apenas na tag**. Assim a `main` protegida nunca recebe push direto
do workflow, e a próxima versão chega às branches por Pull Request.

## Workflows

### 🔍 Validate — `.github/workflows/validate.yml`

Gatilho: push em qualquer branch, exceto `develop` (que tem o pipeline completo), `main` (que só recebe PRs da
`develop`, já validados pelo 🔨 Develop) e `chore/proxima-versao-**` (bump de versão). Três jobs em sequência:

```
Build ──► Testes ──► Cobertura
```

| Job | Steps | Passa adiante |
|---|---|---|
| **Build** | `mvn test-compile` | artefato `build` (código compilado) |
| **Testes** | testes unitários (`-Dtest='**/*Test*'`) → testes de integração (`-Dtest='**/*IT'`) | artefato `execucao-testes` (classes + `jacoco.exec`) |
| **Cobertura** | `mvn jacoco:report` → `mvn jacoco:check@check` (falha abaixo de 80% em `domain` + `application`) | artefato `cobertura-jacoco` |

A cobertura soma os dois passos de teste. Se algum teste falhar, os relatórios vão para o artefato `diagnostico-testes`.

### 🔨 Develop — `.github/workflows/develop.yml`

Gatilho: push na `develop` (merge de PR) ou manual.

O merge do PR de bump de versão (`chore/proxima-versao-*`) **não** executa os jobs: ele só altera o número no `pom.xml`
e já foi validado pelo 🔍 Validate na própria branch. Nesse caso, os jobs aparecem como *skipped*.

```
Build, testes e cobertura ──► Análise estática (CodeQL, PMD e Sonar) ──┐
Vulnerabilidades (Trivy) ──────────────────────────────────────────────┴──► Publicar snapshot no Nexus ──► Excluir branch mergeada
```

| Job | Steps | Artefatos |
|---|---|---|
| Build, testes e cobertura | build → testes unitários → testes de integração → relatório de cobertura → cobertura ≥ 80% → empacotar jar | `resultado-testes`, `cobertura-jacoco`, `oficina-jar` |
| Análise estática (CodeQL, PMD e Sonar) | baixa a cobertura → CodeQL → PMD → CPD → **SonarQube Cloud** (se configurado) | `analise-estatica` |
| Vulnerabilidades (Trivy) | dependências/segredos/IaC → build da imagem → imagem (tabela e SARIF) → Code Scanning | `vulnerabilidades` |
| Publicar snapshot no Nexus | `mvn deploy` da versão `-SNAPSHOT` no repositório `maven-snapshots` | — |
| Excluir branch mergeada | descobre o PR do commit e apaga a branch de origem (ex.: `feature/minha-tarefa`); roda **sempre** ao final, mesmo se algum job falhar | — |

### 📦 Release — `.github/workflows/release.yml`

Gatilho: manual (`Actions → 📦 Release → Run workflow`), sempre a partir da `main`, com campo opcional **versão**.

```
Gerar release ─┬─► Publicar release no Nexus
               └─► Próxima versão
```

| Job | Steps |
|---|---|
| Gerar release | define a versão → aplica no pom → build → testes unitários → testes de integração → cobertura ≥ 80% → empacotar → build e push da imagem no GHCR → tag `vX.Y.Z` → GitHub Release (jar + relatórios) |
| Publicar release no Nexus | checkout da tag `vX.Y.Z` → `mvn deploy` no repositório `maven-releases` (só com `NEXUS_URL` configurada) |
| Próxima versão | atualiza o pom → abre o PR → registra o check `Cobertura` → aprova (só com `RELEASE_TOKEN`) → merge |

### Como gerar uma release

1. Garanta que a `main` está atualizada: faça o merge de um PR `develop → main` (é ele que leva a versão
   `X.Y.Z-SNAPSHOT` atual para a `main`).
2. `Actions → 📦 Release → Run workflow` e, em **Use workflow from**, selecione **`Branch: main`**. Em qualquer outra
   branch o workflow para no primeiro step com erro.
3. Campo **Sobrescrever versão da release**:
   - vazio → usa a versão atual do pom sem `-SNAPSHOT` (`1.0.0-SNAPSHOT` → release `1.0.0`);
   - preenchido → usa a versão informada (`2.0.0` ou `v2.0.0`). Deve seguir `X.Y.Z` e a tag não pode existir.
4. Resultado:
   - tag `v1.0.0` e GitHub Release com `oficina-1.0.0.jar`, relatório de testes e de cobertura;
   - imagem `ghcr.io/aruw/oficina-api:1.0.0` (e `:latest`);
   - artefato `com.aruw:oficina:1.0.0` no Nexus (quando configurado);
   - PR `chore: próxima versão 1.0.1-SNAPSHOT` para a `develop`, aprovado e mergeado automaticamente.

## Integrações opcionais

Os steps abaixo ficam **ignorados** (aparecem como *skipped*) até serem configurados, sem quebrar os workflows.

### SonarQube Cloud (gratuito para repositórios públicos)

1. Entre em <https://sonarcloud.io> com a conta do GitHub e importe o repositório `Aruw/Oficina-Mecanica`.
2. Em `Administration → Analysis Method`, **desative a Automatic Analysis** (a análise é feita pelo workflow, com cobertura).
3. Gere um token em `My Account → Security` e cadastre no GitHub o secret **`SONAR_TOKEN`**
   (`Settings → Secrets and variables → Actions`).
4. Se a organização ou a chave do projeto forem diferentes de `aruw` / `Aruw_Oficina-Mecanica` (valores do `pom.xml`),
   cadastre as variáveis **`SONAR_ORGANIZATION`** e **`SONAR_PROJECT_KEY`**.

O Sonar recebe o relatório de cobertura do JaCoCo e mostra bugs, code smells, duplicação e o Quality Gate.

### Nexus (exemplo do fluxo ideal)

Quando houver um Nexus Repository disponível, cadastre:

| Tipo | Nome | Exemplo |
|---|---|---|
| Variável | `NEXUS_URL` | `https://nexus.minhaempresa.com` |
| Secret | `NEXUS_USERNAME` / `NEXUS_PASSWORD` | usuário com permissão de deploy |

- `develop.yml` publica a versão `X.Y.Z-SNAPSHOT` em `${NEXUS_URL}/repository/maven-snapshots/`, só depois que build,
  análise estática e vulnerabilidades passarem;
- `release.yml` publica a versão final `X.Y.Z` em `${NEXUS_URL}/repository/maven-releases/`.

Sem `NEXUS_URL`, o job e o step de publicação ficam como *skipped*.

### Merge automático do PR da próxima versão

O job **Próxima versão** abre o PR para a `develop` e faz o **merge (squash)** com exclusão da branch. O bump só altera a
versão no `pom.xml` e o código da `develop` já foi validado, então **o 🔍 Validate não roda** nessa branch
(`chore/proxima-versao-**` está em `branches-ignore`). Para atender o check obrigatório da `develop`, o Release publica o
status **`Cobertura`** direto no commit do PR (via API de status, em nome do GitHub Actions).

Opcional: com um secret **`RELEASE_TOKEN`** (fine-grained PAT com **Contents** e **Pull requests** em *Read and write*),
o PR é aberto em nome do dono do token e o `github-actions` também **aprova** o PR — útil se, no futuro, a `develop`
passar a exigir aprovação.

## Configurações necessárias no GitHub

`Settings → Actions → General → Workflow permissions`:

- [x] **Read and write permissions**
- [x] **Allow GitHub Actions to create and approve pull requests** (para abrir e aprovar o PR da próxima versão)

`Settings → General → Pull Requests`:

- [ ] **Allow auto-merge** (opcional; o Release faz o merge direto depois do 🔍 Validate)

`Settings → Rules → Rulesets` (ou *Branches → Branch protection rule*) para `main` e `develop`:

- [x] Require a pull request before merging (com 1 aprovação se houver revisor; trabalhando sozinho, 0 aprovações)
- [x] Require status checks to pass:
  - na `develop`: **`Cobertura`** (último job do 🔍 Validate; só passa se Build e Testes passarem);
  - na `main`: **`Build, testes e cobertura`** (job do 🔨 Develop, que roda no commit da `develop` usado no PR).
- [x] Block force pushes
- [x] Restrict deletions

Mantenha **criação de tags** liberada para o GitHub Actions (não crie ruleset restringindo `v*`), senão o push da tag
da release falha.

`Settings → Code security`: habilite **Code scanning** (os resultados do CodeQL e do Trivy aparecem em
`Security → Code scanning`).

### Secrets e variáveis usados

| Nome | Tipo | Obrigatório | Usado em |
|---|---|---|---|
| `SONAR_TOKEN` | secret | não | 🔨 Develop — SonarQube Cloud |
| `SONAR_ORGANIZATION`, `SONAR_PROJECT_KEY` | variável | não | 🔨 Develop — sobrescreve os valores do `pom.xml` |
| `NEXUS_URL` | variável | não | 🔨 Develop e 📦 Release — habilita o deploy no Nexus |
| `NEXUS_USERNAME`, `NEXUS_PASSWORD` | secret | com `NEXUS_URL` | 🔨 Develop e 📦 Release |
| `RELEASE_TOKEN` | secret | não | 📦 Release — PR aberto em nome do dono do token e aprovado pelo github-actions |
