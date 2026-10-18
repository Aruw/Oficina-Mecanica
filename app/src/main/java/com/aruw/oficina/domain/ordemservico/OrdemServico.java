package com.aruw.oficina.domain.ordemservico;

import com.aruw.oficina.domain.estoque.TipoItemEstoque;
import com.aruw.oficina.domain.ordemservico.evento.OrcamentoAprovado;
import com.aruw.oficina.domain.ordemservico.evento.OrcamentoGerado;
import com.aruw.oficina.domain.ordemservico.evento.OrcamentoRejeitado;
import com.aruw.oficina.domain.ordemservico.evento.OrdemServicoAberta;
import com.aruw.oficina.domain.ordemservico.evento.StatusOrdemServicoAlterado;
import com.aruw.oficina.domain.shared.AggregateRoot;
import com.aruw.oficina.domain.shared.DomainException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "ordens_servico")
public class OrdemServico extends AggregateRoot {

    @Id
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "veiculo_id", nullable = false)
    private UUID veiculoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private StatusOrdemServico status;

    @Column(name = "descricao_problema", length = 1000)
    private String descricaoProblema;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private List<ItemServico> itensServico = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "ordem_servico_id", nullable = false)
    private List<ItemPeca> itensPeca = new ArrayList<>();

    @Embedded
    private Orcamento orcamento;

    @ElementCollection
    @CollectionTable(name = "os_historico_status", joinColumns = @JoinColumn(name = "ordem_servico_id"))
    @OrderBy("dataHora ASC")
    private List<MudancaStatus> historico = new ArrayList<>();

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "diagnostico_iniciado_em")
    private Instant diagnosticoIniciadoEm;

    @Column(name = "execucao_iniciada_em")
    private Instant execucaoIniciadaEm;

    @Column(name = "finalizada_em")
    private Instant finalizadaEm;

    @Column(name = "entregue_em")
    private Instant entregueEm;

    protected OrdemServico() {
    }

    private OrdemServico(UUID clienteId, UUID veiculoId, String descricaoProblema) {
        if (clienteId == null || veiculoId == null) {
            throw new DomainException("Ordem de serviço exige cliente e veículo");
        }
        this.id = UUID.randomUUID();
        this.clienteId = clienteId;
        this.veiculoId = veiculoId;
        this.descricaoProblema = descricaoProblema;
        this.criadaEm = Instant.now();
        this.status = StatusOrdemServico.RECEBIDA;
        this.historico.add(new MudancaStatus(status, criadaEm));
    }

    public static OrdemServico abrir(UUID clienteId, UUID veiculoId, String descricaoProblema) {
        OrdemServico os = new OrdemServico(clienteId, veiculoId, descricaoProblema);
        os.registrarEvento(new OrdemServicoAberta(os.id, clienteId, veiculoId, os.criadaEm));
        return os;
    }

    // ---------- Itens ----------

    public ItemServico adicionarServico(UUID servicoId, String nome, BigDecimal preco, int quantidade) {
        exigirItensEditaveis();
        ItemServico item = new ItemServico(servicoId, nome, preco, quantidade);
        itensServico.add(item);
        return item;
    }

    public ItemPeca adicionarPeca(UUID pecaId, String nome, TipoItemEstoque tipo, BigDecimal precoUnitario, int quantidade) {
        exigirItensEditaveis();
        ItemPeca item = new ItemPeca(pecaId, nome, tipo, precoUnitario, quantidade);
        itensPeca.add(item);
        return item;
    }

    public void removerItemServico(UUID itemId) {
        exigirItensEditaveis();
        if (!itensServico.removeIf(i -> i.getId().equals(itemId))) {
            throw new DomainException("Item de serviço não encontrado na OS");
        }
    }

    public void removerItemPeca(UUID itemId) {
        exigirItensEditaveis();
        if (!itensPeca.removeIf(i -> i.getId().equals(itemId))) {
            throw new DomainException("Item de peça/insumo não encontrado na OS");
        }
    }

    private void exigirItensEditaveis() {
        if (!status.permiteAlterarItens()) {
            throw new DomainException("Itens só podem ser alterados com a OS Recebida ou Em diagnóstico (status atual: "
                    + status.getDescricao() + ")");
        }
    }

    public BigDecimal calcularValorServicos() {
        return itensServico.stream().map(ItemServico::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularValorPecas() {
        return itensPeca.stream().map(ItemPeca::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularValorTotal() {
        return calcularValorServicos().add(calcularValorPecas());
    }

    // ---------- Ciclo de vida ----------

    public void iniciarDiagnostico() {
        alterarStatus(StatusOrdemServico.EM_DIAGNOSTICO);
        this.diagnosticoIniciadoEm = Instant.now();
    }

    public Orcamento gerarOrcamento() {
        if (itensServico.isEmpty() && itensPeca.isEmpty()) {
            throw new DomainException("Inclua ao menos um serviço ou peça antes de gerar o orçamento");
        }
        alterarStatus(StatusOrdemServico.AGUARDANDO_APROVACAO);
        this.orcamento = Orcamento.gerar(calcularValorServicos(), calcularValorPecas());
        registrarEvento(new OrcamentoGerado(id, clienteId, orcamento.getValorTotal(), Instant.now()));
        return orcamento;
    }

    public void aprovarOrcamento() {
        exigirOrcamentoAguardandoDecisao();
        this.orcamento = orcamento.aprovar();
        alterarStatus(StatusOrdemServico.EM_EXECUCAO);
        this.execucaoIniciadaEm = Instant.now();
        registrarEvento(new OrcamentoAprovado(id, clienteId, orcamento.getValorTotal(), Instant.now()));
    }

    public void rejeitarOrcamento(String motivo) {
        exigirOrcamentoAguardandoDecisao();
        this.orcamento = orcamento.rejeitar(motivo);
        alterarStatus(StatusOrdemServico.FINALIZADA);
        this.finalizadaEm = Instant.now();
        registrarEvento(new OrcamentoRejeitado(id, clienteId, motivo, Instant.now()));
    }

    private void exigirOrcamentoAguardandoDecisao() {
        if (status != StatusOrdemServico.AGUARDANDO_APROVACAO || orcamento == null) {
            throw new TransicaoStatusInvalidaException("Não há orçamento aguardando aprovação nesta OS (status atual: "
                    + status.getDescricao() + ")");
        }
    }

    public void finalizar() {
        alterarStatus(StatusOrdemServico.FINALIZADA);
        this.finalizadaEm = Instant.now();
    }

    public void entregar() {
        alterarStatus(StatusOrdemServico.ENTREGUE);
        this.entregueEm = Instant.now();
    }

    private void alterarStatus(StatusOrdemServico novo) {
        if (!status.podeTransitarPara(novo)) {
            throw new TransicaoStatusInvalidaException(status, novo);
        }
        StatusOrdemServico anterior = this.status;
        this.status = novo;
        Instant agora = Instant.now();
        historico.add(new MudancaStatus(novo, agora));
        registrarEvento(new StatusOrdemServicoAlterado(id, anterior, novo, agora));
    }

    public Optional<Duration> tempoExecucao() {
        if (execucaoIniciadaEm == null || finalizadaEm == null) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(execucaoIniciadaEm, finalizadaEm));
    }

    public boolean pertenceAoCliente(UUID clienteId) {
        return this.clienteId.equals(clienteId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public UUID getVeiculoId() {
        return veiculoId;
    }

    public StatusOrdemServico getStatus() {
        return status;
    }

    public String getDescricaoProblema() {
        return descricaoProblema;
    }

    public List<ItemServico> getItensServico() {
        return Collections.unmodifiableList(itensServico);
    }

    public List<ItemPeca> getItensPeca() {
        return Collections.unmodifiableList(itensPeca);
    }

    public Optional<Orcamento> getOrcamento() {
        return Optional.ofNullable(orcamento);
    }

    public List<MudancaStatus> getHistorico() {
        return Collections.unmodifiableList(historico);
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }

    public Instant getDiagnosticoIniciadoEm() {
        return diagnosticoIniciadoEm;
    }

    public Instant getExecucaoIniciadaEm() {
        return execucaoIniciadaEm;
    }

    public Instant getFinalizadaEm() {
        return finalizadaEm;
    }

    public Instant getEntregueEm() {
        return entregueEm;
    }
}
