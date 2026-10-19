# Linguagem Ubíqua — Oficina Mecânica

Termos usados igualmente pela oficina (atendentes, mecânicos, gestão), pelos clientes e pelo código.
A coluna **No código** aponta onde cada termo está implementado (pacote base `com.aruw.oficina`).

## Glossário

| Termo | Definição no negócio | No código |
|---|---|---|
| **Ordem de Serviço (OS)** | Documento que registra o atendimento de um veículo, do recebimento até a entrega. É o centro do negócio. | `domain.ordemservico.OrdemServico` (raiz de agregado) |
| **Cliente** | Pessoa física ou jurídica dona do veículo, identificada por CPF ou CNPJ. | `domain.cliente.Cliente` |
| **Documento** | CPF (11 dígitos) ou CNPJ (14 caracteres, numérico ou alfanumérico) válido do cliente. | `domain.shared.Documento` (objeto de valor) |
| **Veículo** | Automóvel atendido, identificado pela placa, com marca, modelo e ano. Pertence a um cliente. | `domain.veiculo.Veiculo` |
| **Placa** | Identificação do veículo no padrão antigo (ABC1234) ou Mercosul (ABC1D23). | `domain.shared.Placa` (objeto de valor) |
| **Serviço** | Mão de obra oferecida no catálogo (ex.: troca de óleo, alinhamento), com preço e tempo estimado. | `domain.catalogo.Servico` |
| **Peça** | Componente substituído no veículo (ex.: filtro, pastilha). | `domain.estoque.Peca` com `TipoItemEstoque.PECA` |
| **Insumo** | Material consumido na execução (ex.: óleo, fluido de freio). | `domain.estoque.Peca` com `TipoItemEstoque.INSUMO` |
| **Estoque** | Saldo disponível de cada peça/insumo. Nunca pode ficar negativo. | `Peca.quantidadeEstoque` |
| **Estoque mínimo** | Saldo a partir do qual a oficina precisa repor o item. | `Peca.estoqueMinimo`, evento `EstoqueAbaixoDoMinimo` |
| **Movimentação de estoque** | Registro de entrada (reposição) ou saída (baixa por OS) de um item. | `domain.estoque.MovimentacaoEstoque` |
| **Entrada de estoque** | Reposição do saldo (compra, ajuste). | `Peca.darEntrada`, `POST /pecas/{id}/entradas` |
| **Baixa de estoque** | Retirada do saldo para uso em uma OS, feita quando o orçamento é aprovado. | `Peca.darBaixa`, `PecaService.baixarParaOrdemServico` |
| **Item de serviço** | Serviço incluído na OS, com preço congelado no momento da inclusão. | `domain.ordemservico.ItemServico` |
| **Item de peça** | Peça/insumo incluído na OS, com preço congelado no momento da inclusão. | `domain.ordemservico.ItemPeca` |
| **Valor estimado** | Soma corrente dos itens da OS antes de o orçamento ser gerado. | `OrdemServico.calcularValorTotal()` |
| **Orçamento** | Consolidação dos valores de serviços e peças enviada ao cliente para decisão. | `domain.ordemservico.Orcamento` (objeto de valor) |
| **Aprovação do orçamento** | Autorização do cliente para executar os serviços. Inicia a execução. | `OrdemServico.aprovarOrcamento()` |
| **Rejeição do orçamento** | Recusa do cliente. A OS é encerrada sem execução. | `OrdemServico.rejeitarOrcamento()` |
| **Situação do orçamento** | Pendente, Aprovado ou Rejeitado. | `SituacaoOrcamento` |
| **Status da OS** | Etapa atual: Recebida, Em diagnóstico, Aguardando aprovação, Em execução, Finalizada, Entregue. | `StatusOrdemServico` |
| **Recebida** | Veículo deixado na oficina; OS aberta. | `RECEBIDA` |
| **Em diagnóstico** | Mecânico avalia o veículo e define serviços e peças. | `EM_DIAGNOSTICO` |
| **Aguardando aprovação** | Orçamento enviado; aguardando decisão do cliente. | `AGUARDANDO_APROVACAO` |
| **Em execução** | Orçamento aprovado; serviços sendo realizados. | `EM_EXECUCAO` |
| **Finalizada** | Serviços concluídos (ou orçamento rejeitado); veículo pronto para retirada. | `FINALIZADA` |
| **Entregue** | Veículo devolvido ao cliente. Estado final. | `ENTREGUE` |
| **Histórico de status** | Linha do tempo das mudanças de status, com data e hora. | `MudancaStatus` (objeto de valor) |
| **Transição de status** | Mudança permitida entre dois status, segundo as regras do ciclo de vida. | `StatusOrdemServico.podeTransitarPara` |
| **Prioridade de atendimento** | Ordem da fila: Em execução → Aguardando aprovação → Em diagnóstico → Recebida; dentro do mesmo status, a mais antiga primeiro. | `StatusOrdemServico.prioridade()` |
| **Tempo de execução** | Tempo entre a aprovação do orçamento e a finalização da OS. | `OrdemServico.tempoExecucao()` |
| **Tempo médio de execução** | Média dos tempos de execução, geral e por serviço, comparada ao tempo estimado. | `MetricasService`, `TempoMedioExecucao` |
| **Acompanhamento** | Consulta da OS pelo cliente, informando o número da OS e o próprio CPF/CNPJ. | `GET /api/public/ordens-servico/{id}` |
| **Atendente** | Usuário interno que abre OS e cadastra clientes, veículos e itens. | perfil `ATENDENTE` |
| **Administrador** | Usuário interno com acesso total, inclusive exclusões. | perfil `ADMIN` |
| **Mecânico** | Responsável pelo diagnóstico e pela execução (no MVP, as ações são registradas por um usuário interno). | — |

## Frases do domínio (como o negócio fala)

- "O **atendente abre uma OS** identificando o **cliente pelo CPF/CNPJ** e o **veículo pela placa**."
- "Durante o **diagnóstico**, o mecânico **inclui serviços, peças e insumos**."
- "Ao **gerar o orçamento**, ele é **enviado ao cliente** e a OS passa a **aguardar aprovação**."
- "Quando o cliente **aprova**, as peças são **baixadas do estoque** e a OS entra **em execução**."
- "Se o cliente **rejeita**, a OS é **finalizada sem execução** e o veículo fica disponível para retirada."
- "Depois de **finalizada**, a OS só pode ir para **entregue**."
- "Itens só podem ser alterados enquanto a OS está **Recebida** ou **Em diagnóstico**."
