# Event Storming

Sessão de *Big Picture* refinada até o nível de *Design Level* para os três fluxos exigidos.
Legenda das cores (padrão Event Storming), usada também no quadro do Miro e em [`event-storming.pdf`](event-storming.pdf):

| Cor | Elemento |
|---|---|
| 🟧 Laranja | Evento de domínio (passado) |
| 🟦 Azul | Comando |
| 🟨 Amarelo pequeno | Ator |
| 🟨 Amarelo grande | Agregado |
| 🟪 Lilás | Política / regra ("sempre que… então…") |
| 🟩 Verde | Modelo de leitura (o que o ator consulta para decidir) |
| 🟥 Rosa | Ponto de atenção / *hotspot* |

> Cada evento abaixo existe no código (`domain/**/evento`) ou corresponde a uma mudança de estado explícita no agregado.

---

## 1. Criação da Ordem de Serviço

```mermaid
flowchart LR
    A1([👤 Atendente]):::ator --> C1[Abrir OS]:::cmd
    RM1[(Cliente por CPF/CNPJ<br/>Veículo por placa)]:::leitura -.-> C1
    C1 --> AG1[[Ordem de Serviço]]:::agg
    AG1 --> E1{{OS aberta}}:::evt
    C1 -. cliente não existe .-> E0{{Cliente cadastrado}}:::evt
    C1 -. veículo não existe .-> E00{{Veículo cadastrado}}:::evt
    E1 --> P1>Política: OS nasce como Recebida]:::pol
    A2([👤 Atendente / Mecânico]):::ator --> C2[Iniciar diagnóstico]:::cmd --> AG1
    AG1 --> E2{{Diagnóstico iniciado}}:::evt
    A2 --> C3[Incluir serviço]:::cmd --> AG1
    AG1 --> E3{{Serviço incluído}}:::evt
    A2 --> C4[Incluir peça/insumo]:::cmd --> AG1
    RM2[(Saldo em estoque)]:::leitura -.-> C4
    AG1 --> E4{{Peça incluída}}:::evt
    A2 --> C5[Gerar orçamento]:::cmd --> AG1
    AG1 --> E5{{Orçamento gerado}}:::evt
    E5 --> P2>Política: ao gerar orçamento,<br/>enviar ao cliente e<br/>mudar para Aguardando aprovação]:::pol
    P2 --> E6{{Orçamento enviado ao cliente}}:::evt
    H1[/Hotspot: cliente sem cadastro?<br/>→ cadastrar no ato com nome/]:::hot -.-> C1

    classDef evt fill:#ffb347,stroke:#c77700,color:#000
    classDef cmd fill:#7ec8ff,stroke:#1f6fb2,color:#000
    classDef agg fill:#fff176,stroke:#b8a400,color:#000
    classDef ator fill:#fffde7,stroke:#b8a400,color:#000
    classDef pol fill:#d1a6ff,stroke:#7a3fb5,color:#000
    classDef leitura fill:#a5e6a5,stroke:#2e7d32,color:#000
    classDef hot fill:#ff9fb2,stroke:#c2185b,color:#000
```

| Ator | Comando | Agregado | Evento | Política / regra |
|---|---|---|---|---|
| Atendente | Abrir OS | Ordem de Serviço | `OrdemServicoAberta` | Cliente identificado por CPF/CNPJ válido; veículo por placa válida; veículo deve pertencer ao cliente |
| Atendente | (implícito) Cadastrar cliente | Cliente | Cliente cadastrado | Só cadastra se o nome for informado; documento único |
| Atendente | (implícito) Cadastrar veículo | Veículo | Veículo cadastrado | Exige marca, modelo e ano (1900 … ano atual + 1); placa única |
| Mecânico | Iniciar diagnóstico | Ordem de Serviço | `StatusOrdemServicoAlterado` (→ Em diagnóstico) | Só a partir de Recebida |
| Mecânico | Incluir serviço | Ordem de Serviço | Serviço incluído | Serviço deve estar ativo; preço é congelado |
| Mecânico | Incluir peça/insumo | Ordem de Serviço | Peça incluída | Saldo deve cobrir a quantidade total na OS; preço congelado |
| Mecânico | Gerar orçamento | Ordem de Serviço | `OrcamentoGerado` | Precisa de ao menos 1 item; status muda automaticamente para Aguardando aprovação; cliente é notificado |

---

## 2. Acompanhamento da Ordem de Serviço

```mermaid
flowchart LR
    CL([👤 Cliente]):::ator --> C1[Consultar andamento]:::cmd
    RM1[(OS: status, itens,<br/>orçamento, histórico)]:::leitura -.-> CL
    CL --> C2[Aprovar orçamento]:::cmd --> AG[[Ordem de Serviço]]:::agg
    AG --> E1{{Orçamento aprovado}}:::evt
    E1 --> P1>Política: ao aprovar,<br/>baixar peças do estoque<br/>e iniciar execução]:::pol
    P1 --> E2{{Execução iniciada}}:::evt
    P1 --> E2b{{Estoque baixado}}:::evt
    CL --> C3[Rejeitar orçamento]:::cmd --> AG
    AG --> E3{{Orçamento rejeitado}}:::evt
    E3 --> P2>Política: ao rejeitar,<br/>finalizar OS sem execução]:::pol
    P2 --> E4{{OS finalizada}}:::evt
    M([👤 Mecânico]):::ator --> C4[Finalizar serviço]:::cmd --> AG
    AG --> E5{{OS finalizada}}:::evt
    E5 --> P3>Política: tempo de execução<br/>entra no indicador de tempo médio]:::pol
    A([👤 Atendente]):::ator --> C5[Entregar veículo]:::cmd --> AG
    AG --> E6{{Veículo entregue}}:::evt
    G([👤 Gestor]):::ator -.-> RM2[(Tempo médio de execução)]:::leitura
    H[/Hotspot: cliente consulta OS de outro?<br/>→ exige CPF/CNPJ do titular/]:::hot -.-> C1

    classDef evt fill:#ffb347,stroke:#c77700,color:#000
    classDef cmd fill:#7ec8ff,stroke:#1f6fb2,color:#000
    classDef agg fill:#fff176,stroke:#b8a400,color:#000
    classDef ator fill:#fffde7,stroke:#b8a400,color:#000
    classDef pol fill:#d1a6ff,stroke:#7a3fb5,color:#000
    classDef leitura fill:#a5e6a5,stroke:#2e7d32,color:#000
    classDef hot fill:#ff9fb2,stroke:#c2185b,color:#000
```

| Ator | Comando | Evento | Política / regra |
|---|---|---|---|
| Cliente | Consultar andamento | — (leitura) | Exige número da OS + CPF/CNPJ do titular; documento divergente responde 404 |
| Cliente / Atendente | Aprovar orçamento | `OrcamentoAprovado`, `StatusOrdemServicoAlterado` (→ Em execução) | Só com orçamento Pendente; baixa de estoque na mesma transação — sem saldo, nada é confirmado |
| Cliente / Atendente | Rejeitar orçamento | `OrcamentoRejeitado`, `StatusOrdemServicoAlterado` (→ Finalizada) | Estoque não é alterado; OS não entra no indicador de tempo |
| Mecânico | Finalizar serviço | `StatusOrdemServicoAlterado` (→ Finalizada) | Só a partir de Em execução |
| Atendente | Entregar veículo | `StatusOrdemServicoAlterado` (→ Entregue) | Só a partir de Finalizada; estado final |
| Gestor | Consultar tempo médio | — (leitura) | Considera OS com execução iniciada e finalizada |

---

## 3. Gestão de peças e insumos

```mermaid
flowchart LR
    G([👤 Administrador]):::ator --> C1[Cadastrar peça/insumo]:::cmd --> AG[[Peça / Insumo]]:::agg
    AG --> E1{{Peça cadastrada}}:::evt
    E1 --> P0>Política: saldo inicial gera<br/>movimentação de entrada]:::pol
    G --> C2[Registrar entrada]:::cmd --> AG
    AG --> E2{{Estoque reposto}}:::evt
    OS{{Orçamento aprovado}}:::evt --> P1>Política: baixar itens da OS]:::pol
    P1 --> C3[Dar baixa]:::cmd --> AG
    AG --> E3{{Estoque baixado}}:::evt
    E3 --> P2>Política: saldo ≤ mínimo<br/>→ alertar reposição]:::pol
    P2 --> E4{{Estoque abaixo do mínimo}}:::evt
    G -.-> RM[(Itens abaixo do mínimo<br/>Histórico de movimentações)]:::leitura
    H[/Hotspot: duas OS aprovadas ao mesmo tempo<br/>→ lock otimista @Version/]:::hot -.-> C3

    classDef evt fill:#ffb347,stroke:#c77700,color:#000
    classDef cmd fill:#7ec8ff,stroke:#1f6fb2,color:#000
    classDef agg fill:#fff176,stroke:#b8a400,color:#000
    classDef ator fill:#fffde7,stroke:#b8a400,color:#000
    classDef pol fill:#d1a6ff,stroke:#7a3fb5,color:#000
    classDef leitura fill:#a5e6a5,stroke:#2e7d32,color:#000
    classDef hot fill:#ff9fb2,stroke:#c2185b,color:#000
```

| Ator | Comando | Evento | Política / regra |
|---|---|---|---|
| Administrador | Cadastrar peça/insumo | Peça cadastrada | Código único; preço ≥ 0; saldo inicial ≥ 0 gera movimentação de entrada |
| Administrador | Registrar entrada | Estoque reposto | Quantidade > 0; gera `MovimentacaoEstoque` ENTRADA |
| Sistema (política) | Dar baixa | Estoque baixado | Disparado pela aprovação do orçamento; saldo nunca negativo (`EstoqueInsuficienteException`); gera movimentação SAÍDA vinculada à OS |
| Sistema (política) | — | `EstoqueAbaixoDoMinimo` | Após a baixa, se saldo ≤ mínimo, alerta de reposição |

---

## Consolidação

### Eventos de domínio
`OrdemServicoAberta` · `StatusOrdemServicoAlterado` · `OrcamentoGerado` · `OrcamentoAprovado` · `OrcamentoRejeitado` · `EstoqueAbaixoDoMinimo`
(+ eventos de estado sem classe própria: Cliente cadastrado, Veículo cadastrado, Serviço/Peça incluída, Estoque reposto, Estoque baixado — registrados como mudança no agregado e/ou `MovimentacaoEstoque`).

### Comandos
Abrir OS · Iniciar diagnóstico · Incluir/Remover serviço · Incluir/Remover peça · Gerar orçamento · Aprovar orçamento · Rejeitar orçamento · Finalizar OS · Entregar veículo · Cadastrar/Atualizar/Remover cliente, veículo, serviço, peça · Registrar entrada de estoque · Autenticar usuário.

### Políticas
1. OS sempre nasce **Recebida**.
2. Itens só podem mudar em **Recebida** ou **Em diagnóstico**.
3. Gerar orçamento ⇒ **Aguardando aprovação** + notificação do cliente.
4. Aprovar ⇒ baixa de estoque **atômica** + **Em execução**.
5. Rejeitar ⇒ **Finalizada** sem execução e sem baixa.
6. Saldo ≤ mínimo após baixa ⇒ alerta de reposição.
7. Cliente só acessa a OS com o próprio CPF/CNPJ.
8. Fila de atendimento ordenada por prioridade de status e antiguidade.

### Atores
Atendente · Mecânico · Administrador/Gestor · Cliente · Sistema (políticas automáticas).

### Agregados
Ordem de Serviço · Cliente · Veículo · Serviço · Peça/Insumo.
