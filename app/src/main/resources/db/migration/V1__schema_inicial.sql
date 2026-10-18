-- Esquema inicial da oficina (compatível com PostgreSQL e H2 em modo PostgreSQL)

CREATE TABLE usuarios (
    id          UUID         PRIMARY KEY,
    username    VARCHAR(60)  NOT NULL UNIQUE,
    senha_hash  VARCHAR(100) NOT NULL,
    perfil      VARCHAR(20)  NOT NULL,
    ativo       BOOLEAN      NOT NULL
);

CREATE TABLE clientes (
    id              UUID         PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    documento       VARCHAR(14)  NOT NULL UNIQUE,
    tipo_documento  VARCHAR(4)   NOT NULL,
    email           VARCHAR(150),
    telefone        VARCHAR(20),
    criado_em       TIMESTAMP WITH TIME ZONE NOT NULL,
    versao          BIGINT
);

CREATE TABLE veiculos (
    id          UUID        PRIMARY KEY,
    placa       VARCHAR(7)  NOT NULL UNIQUE,
    marca       VARCHAR(60) NOT NULL,
    modelo      VARCHAR(80) NOT NULL,
    ano         INTEGER     NOT NULL,
    cliente_id  UUID        NOT NULL REFERENCES clientes (id),
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    versao      BIGINT
);
CREATE INDEX idx_veiculos_cliente ON veiculos (cliente_id);

CREATE TABLE servicos (
    id                      UUID          PRIMARY KEY,
    nome                    VARCHAR(120)  NOT NULL,
    descricao               VARCHAR(500),
    preco                   NUMERIC(12,2) NOT NULL,
    tempo_estimado_minutos  INTEGER       NOT NULL,
    ativo                   BOOLEAN       NOT NULL,
    versao                  BIGINT
);

CREATE TABLE pecas (
    id                  UUID          PRIMARY KEY,
    codigo              VARCHAR(40)   NOT NULL UNIQUE,
    nome                VARCHAR(120)  NOT NULL,
    descricao           VARCHAR(500),
    tipo                VARCHAR(10)   NOT NULL,
    preco_unitario      NUMERIC(12,2) NOT NULL,
    quantidade_estoque  INTEGER       NOT NULL CHECK (quantidade_estoque >= 0),
    estoque_minimo      INTEGER       NOT NULL,
    versao              BIGINT
);

CREATE TABLE movimentacoes_estoque (
    id                UUID        PRIMARY KEY,
    peca_id           UUID        NOT NULL REFERENCES pecas (id) ON DELETE CASCADE,
    tipo              VARCHAR(10) NOT NULL,
    quantidade        INTEGER     NOT NULL,
    motivo            VARCHAR(255),
    ordem_servico_id  UUID,
    data_hora         TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_movimentacoes_peca ON movimentacoes_estoque (peca_id);

CREATE TABLE ordens_servico (
    id                        UUID          PRIMARY KEY,
    cliente_id                UUID          NOT NULL REFERENCES clientes (id),
    veiculo_id                UUID          NOT NULL REFERENCES veiculos (id),
    status                    VARCHAR(25)   NOT NULL,
    descricao_problema        VARCHAR(1000),
    orcamento_valor_servicos  NUMERIC(12,2),
    orcamento_valor_pecas     NUMERIC(12,2),
    orcamento_valor_total     NUMERIC(12,2),
    orcamento_gerado_em       TIMESTAMP WITH TIME ZONE,
    orcamento_situacao        VARCHAR(10),
    orcamento_decidido_em     TIMESTAMP WITH TIME ZONE,
    orcamento_observacao      VARCHAR(500),
    criada_em                 TIMESTAMP WITH TIME ZONE NOT NULL,
    diagnostico_iniciado_em   TIMESTAMP WITH TIME ZONE,
    execucao_iniciada_em      TIMESTAMP WITH TIME ZONE,
    finalizada_em             TIMESTAMP WITH TIME ZONE,
    entregue_em               TIMESTAMP WITH TIME ZONE,
    versao                    BIGINT
);
CREATE INDEX idx_os_status ON ordens_servico (status);
CREATE INDEX idx_os_cliente ON ordens_servico (cliente_id);

CREATE TABLE os_itens_servico (
    id                UUID          PRIMARY KEY,
    ordem_servico_id  UUID          NOT NULL REFERENCES ordens_servico (id) ON DELETE CASCADE,
    servico_id        UUID          NOT NULL,
    nome              VARCHAR(120)  NOT NULL,
    preco_unitario    NUMERIC(12,2) NOT NULL,
    quantidade        INTEGER       NOT NULL
);

CREATE TABLE os_itens_peca (
    id                UUID          PRIMARY KEY,
    ordem_servico_id  UUID          NOT NULL REFERENCES ordens_servico (id) ON DELETE CASCADE,
    peca_id           UUID          NOT NULL,
    nome              VARCHAR(120)  NOT NULL,
    tipo              VARCHAR(10)   NOT NULL,
    preco_unitario    NUMERIC(12,2) NOT NULL,
    quantidade        INTEGER       NOT NULL
);

CREATE TABLE os_historico_status (
    ordem_servico_id  UUID        NOT NULL REFERENCES ordens_servico (id) ON DELETE CASCADE,
    status            VARCHAR(25) NOT NULL,
    data_hora         TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_historico_os ON os_historico_status (ordem_servico_id);
