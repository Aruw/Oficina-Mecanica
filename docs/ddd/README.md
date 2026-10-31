# Documentação DDD

| Artefato | Arquivo |
|---|---|
| Quadro no Miro | [https://miro.com/app/board/uXjVEToLmPQ=/](https://miro.com/app/board/uXjVEToLmPQ=/) |
| Event Storming (3 fluxos) | [event-storming.md](event-storming.md) · [event-storming.pdf](event-storming.pdf) |
| Diagramas (contextos, agregados, estados, sequência, camadas) | [diagramas.md](diagramas.md) · [diagramas.pdf](diagramas.pdf) |
| Linguagem Ubíqua e glossário | [linguagem-ubiqua.md](linguagem-ubiqua.md) |

## Relação entre a documentação e o código

Pacote base: `app/src/main/java/com/aruw/oficina`.

| Conceito DDD | Elemento | Implementação |
|---|---|---|
| Bounded Context (core) | Ordem de Serviço | `domain/ordemservico`, `application/ordemservico` |
| Bounded Context | Estoque | `domain/estoque`, `application/estoque` |
| Bounded Context | Catálogo | `domain/catalogo`, `application/catalogo` |
| Bounded Context | Atendimento | `domain/cliente`, `domain/veiculo` |
| Bounded Context (genérico) | Identidade e Acesso | `infrastructure/security` |
| Raiz de agregado | OrdemServico, Cliente, Veiculo, Servico, Peca | estendem `domain/shared/AggregateRoot` (eventos + `@Version`) |
| Entidade interna | ItemServico, ItemPeca | `domain/ordemservico` — só alteradas via `OrdemServico` |
| Entidade | MovimentacaoEstoque | `domain/estoque/MovimentacaoEstoque` |
| Objeto de valor | Documento (CPF/CNPJ), Placa | `domain/shared` — imutáveis, validados na criação, igualdade por valor |
| Objeto de valor | Orcamento, MudancaStatus | `domain/ordemservico` — `aprovar()`/`rejeitar()` retornam nova instância |
| Evento de domínio | OrdemServicoAberta, StatusOrdemServicoAlterado, OrcamentoGerado, OrcamentoAprovado, OrcamentoRejeitado | `domain/ordemservico/evento` |
| Evento de domínio | EstoqueAbaixoDoMinimo | `domain/estoque/evento` |
| Política | Gerar orçamento ⇒ Aguardando aprovação + notificar | `OrdemServico.gerarOrcamento()` + `infrastructure/notificacao/NotificacaoListener` |
| Política | Aprovar ⇒ baixar estoque + Em execução | `OrdemServicoService.aprovar()` (mesma transação) |
| Política | Rejeitar ⇒ Finalizada sem execução | `OrdemServico.rejeitarOrcamento()` |
| Política | Saldo ≤ mínimo ⇒ alerta | `Peca.darBaixa()` registra `EstoqueAbaixoDoMinimo` |
| Regra de transição | Máquina de estados | `StatusOrdemServico.proximosPermitidos()` |
| Repositório (porta) | ClienteRepository, OrdemServicoRepository… | interfaces em `domain/**` |
| Repositório (adaptador) | Spring Data JPA | `infrastructure/persistence/Jpa*Repository` |
| Serviço de aplicação | Casos de uso | `application/**/*Service` |
| Modelo de leitura | Detalhe da OS, Tempo médio | `OrdemServicoDetalhe`, `TempoMedioExecucao` |
| Open Host Service | API do cliente | `api/publico/AcompanhamentoController` (`/api/public/**`) |
| Comandos | Endpoints `POST` | `api/ordemservico/OrdemServicoController` |

## Testes que verificam as regras documentadas

| Regra | Teste |
|---|---|
| Validação de CPF/CNPJ (inclusive alfanumérico) | `DocumentoTest` |
| Validação de placa antiga/Mercosul | `PlacaTest` |
| Transições de status e regras do orçamento | `OrdemServicoTest` |
| Estoque nunca negativo / alerta de mínimo | `PecaTest` |
| Abertura de OS, baixa de estoque, acesso do cliente | `OrdemServicoServiceTest` |
| Fluxo completo ponta a ponta via API | `integration/OrdemServicoFluxoIT` |
