# Diagramas DDD

## 1. Subdomínios e Bounded Contexts

| Bounded Context | Tipo de subdomínio | Responsabilidade | Pacotes |
|---|---|---|---|
| **Ordem de Serviço** | Núcleo (*core*) | Ciclo de vida da OS, orçamento, aprovação, status, métricas | `domain.ordemservico`, `application.ordemservico` |
| **Estoque** | Suporte | Peças/insumos, saldo, movimentações, alerta de mínimo | `domain.estoque`, `application.estoque` |
| **Catálogo de Serviços** | Suporte | Serviços oferecidos, preço e tempo estimado | `domain.catalogo`, `application.catalogo` |
| **Clientes e Veículos (Atendimento)** | Suporte | Cadastro e identificação por CPF/CNPJ e placa | `domain.cliente`, `domain.veiculo` |
| **Identidade e Acesso** | Genérico | Usuários internos, autenticação JWT | `infrastructure.security` |

> Por ser um MVP monolítico, os contextos convivem no mesmo deployable e banco, separados por pacotes.
> As fronteiras são respeitadas referenciando outros agregados **apenas por identidade (UUID)** e
> copiando os dados necessários (nome e preço congelados nos itens da OS).

## 2. Mapa de Contextos

```mermaid
flowchart TB
    subgraph CORE["🟥 Core — Ordem de Serviço"]
        OS[Ordem de Serviço<br/>Orçamento · Status · Métricas]
    end
    subgraph SUP1["Suporte — Estoque"]
        EST[Peça / Insumo<br/>Movimentações]
    end
    subgraph SUP2["Suporte — Catálogo"]
        CAT[Serviço]
    end
    subgraph SUP3["Suporte — Atendimento"]
        CLI[Cliente]
        VEI[Veículo]
    end
    subgraph GEN["Genérico — Identidade e Acesso"]
        IAM[Usuário · JWT]
    end
    APP([App do cliente]):::ext

    CAT -- "Customer/Supplier<br/>(OS consome nome e preço)" --> OS
    EST -- "Customer/Supplier<br/>(disponibilidade e baixa)" --> OS
    CLI -- "Conformist<br/>(identificação por CPF/CNPJ)" --> OS
    VEI -- "Conformist<br/>(identificação por placa)" --> OS
    OS -- "Open Host Service<br/>/api/public (Published Language: JSON)" --> APP
    IAM -. "protege /api/admin" .-> OS

    classDef ext fill:#eee,stroke:#777
```

## 3. Agregados, entidades e objetos de valor

```mermaid
classDiagram
    direction TB

    class OrdemServico {
        <<Aggregate Root>>
        UUID id
        UUID clienteId
        UUID veiculoId
        StatusOrdemServico status
        String descricaoProblema
        Instant criadaEm / execucaoIniciadaEm / finalizadaEm / entregueEm
        +abrir(clienteId, veiculoId, descricao)$
        +adicionarServico(...)
        +adicionarPeca(...)
        +iniciarDiagnostico()
        +gerarOrcamento() Orcamento
        +aprovarOrcamento()
        +rejeitarOrcamento(motivo)
        +finalizar()
        +entregar()
        +tempoExecucao() Duration
    }
    class ItemServico {
        <<Entity>>
        UUID id
        UUID servicoId
        String nome
        BigDecimal precoUnitario
        int quantidade
        +subtotal()
    }
    class ItemPeca {
        <<Entity>>
        UUID id
        UUID pecaId
        String nome
        TipoItemEstoque tipo
        BigDecimal precoUnitario
        int quantidade
        +subtotal()
    }
    class Orcamento {
        <<Value Object>>
        BigDecimal valorServicos
        BigDecimal valorPecas
        BigDecimal valorTotal
        SituacaoOrcamento situacao
        +aprovar() Orcamento
        +rejeitar(motivo) Orcamento
    }
    class MudancaStatus {
        <<Value Object>>
        StatusOrdemServico status
        Instant dataHora
    }
    class StatusOrdemServico {
        <<Enum>>
        RECEBIDA
        EM_DIAGNOSTICO
        AGUARDANDO_APROVACAO
        EM_EXECUCAO
        FINALIZADA
        ENTREGUE
    }

    class Cliente {
        <<Aggregate Root>>
        UUID id
        String nome
        String email
        String telefone
    }
    class Documento {
        <<Value Object>>
        String valor
        Tipo tipo (CPF|CNPJ)
        +of(String)$
    }
    class Veiculo {
        <<Aggregate Root>>
        UUID id
        String marca
        String modelo
        int ano
        UUID clienteId
    }
    class Placa {
        <<Value Object>>
        String valor
        +of(String)$
        +isMercosul()
    }
    class Servico {
        <<Aggregate Root>>
        UUID id
        String nome
        BigDecimal preco
        int tempoEstimadoMinutos
        boolean ativo
    }
    class Peca {
        <<Aggregate Root>>
        UUID id
        String codigo
        String nome
        TipoItemEstoque tipo
        BigDecimal precoUnitario
        int quantidadeEstoque
        int estoqueMinimo
        +darEntrada(qtd)
        +darBaixa(qtd)
    }
    class MovimentacaoEstoque {
        <<Entity>>
        UUID pecaId
        Tipo tipo (ENTRADA|SAIDA)
        int quantidade
        UUID ordemServicoId
    }

    OrdemServico "1" *-- "0..*" ItemServico
    OrdemServico "1" *-- "0..*" ItemPeca
    OrdemServico "1" *-- "0..1" Orcamento
    OrdemServico "1" *-- "1..*" MudancaStatus
    OrdemServico --> StatusOrdemServico
    Cliente *-- Documento
    Veiculo *-- Placa
    OrdemServico ..> Cliente : clienteId
    OrdemServico ..> Veiculo : veiculoId
    Veiculo ..> Cliente : clienteId
    ItemServico ..> Servico : servicoId
    ItemPeca ..> Peca : pecaId
    MovimentacaoEstoque ..> Peca : pecaId
```

Linhas tracejadas = referência **por identidade** entre agregados (sem navegação de objeto).

## 4. Máquina de estados da OS

```mermaid
stateDiagram-v2
    [*] --> RECEBIDA : Abrir OS
    RECEBIDA --> EM_DIAGNOSTICO : Iniciar diagnóstico
    EM_DIAGNOSTICO --> AGUARDANDO_APROVACAO : Gerar orçamento (automático)
    AGUARDANDO_APROVACAO --> EM_EXECUCAO : Orçamento aprovado (automático + baixa de estoque)
    AGUARDANDO_APROVACAO --> FINALIZADA : Orçamento rejeitado (automático)
    EM_EXECUCAO --> FINALIZADA : Finalizar
    FINALIZADA --> ENTREGUE : Entregar veículo
    ENTREGUE --> [*]

    note right of RECEBIDA : Itens podem ser incluídos/removidos
    note right of EM_DIAGNOSTICO : Itens podem ser incluídos/removidos
```

Qualquer outra transição lança `TransicaoStatusInvalidaException` (HTTP 409).

## 5. Sequência — aprovação do orçamento pelo cliente

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente (app)
    participant API as AcompanhamentoController
    participant S as OrdemServicoService
    participant OS as OrdemServico (agregado)
    participant E as PecaService / Peca
    participant DB as PostgreSQL
    participant N as NotificacaoListener

    C->>API: POST /api/public/ordens-servico/{id}/orcamento/aprovacao {documento}
    API->>S: aprovarPeloCliente(id, documento)
    Note over S,DB: @Transactional — tudo ou nada
    S->>DB: busca OS e confere CPF/CNPJ do titular
    S->>OS: aprovarOrcamento()
    OS-->>S: status EM_EXECUCAO + eventos
    loop cada ItemPeca
        S->>E: baixarParaOrdemServico(pecaId, qtd, osId)
        E->>E: darBaixa (saldo ≥ qtd, senão exceção → rollback)
        E->>DB: salva Peca (@Version) + MovimentacaoEstoque SAÍDA
    end
    S->>DB: salva OS
    DB-->>S: commit
    S-->>N: OrcamentoAprovado / StatusOrdemServicoAlterado (AFTER_COMMIT)
    S-->>API: OrdemServicoDetalhe
    API-->>C: 200 OK
```

## 6. Arquitetura em camadas

```mermaid
flowchart TB
    subgraph API["api — Apresentação (REST, DTOs, validação, Swagger, erros RFC 7807)"]
    end
    subgraph APP["application — Casos de uso (@Transactional, orquestração, publicação de eventos)"]
    end
    subgraph DOM["domain — Modelo (agregados, VOs, eventos, regras, portas de repositório)"]
    end
    subgraph INF["infrastructure — Spring Data JPA, segurança JWT, notificações, configuração"]
    end
    API --> APP --> DOM
    INF -. implementa portas .-> DOM
    API -.-> INF
```
