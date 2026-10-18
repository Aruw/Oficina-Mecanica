package com.aruw.oficina.domain.ordemservico;

import com.aruw.oficina.domain.shared.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;
import java.time.Instant;

@Embeddable
public class Orcamento {

    @Column(name = "orcamento_valor_servicos", precision = 12, scale = 2)
    private BigDecimal valorServicos;

    @Column(name = "orcamento_valor_pecas", precision = 12, scale = 2)
    private BigDecimal valorPecas;

    @Column(name = "orcamento_valor_total", precision = 12, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "orcamento_gerado_em")
    private Instant geradoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "orcamento_situacao", length = 10)
    private SituacaoOrcamento situacao;

    @Column(name = "orcamento_decidido_em")
    private Instant decididoEm;

    @Column(name = "orcamento_observacao", length = 500)
    private String observacao;

    protected Orcamento() {
    }

    private Orcamento(BigDecimal valorServicos, BigDecimal valorPecas, Instant geradoEm,
                      SituacaoOrcamento situacao, Instant decididoEm, String observacao) {
        this.valorServicos = valorServicos;
        this.valorPecas = valorPecas;
        this.valorTotal = valorServicos.add(valorPecas);
        this.geradoEm = geradoEm;
        this.situacao = situacao;
        this.decididoEm = decididoEm;
        this.observacao = observacao;
    }

    public static Orcamento gerar(BigDecimal valorServicos, BigDecimal valorPecas) {
        return new Orcamento(valorServicos, valorPecas, Instant.now(), SituacaoOrcamento.PENDENTE, null, null);
    }

    public Orcamento aprovar() {
        exigirPendente();
        return new Orcamento(valorServicos, valorPecas, geradoEm, SituacaoOrcamento.APROVADO, Instant.now(), null);
    }

    public Orcamento rejeitar(String motivo) {
        exigirPendente();
        return new Orcamento(valorServicos, valorPecas, geradoEm, SituacaoOrcamento.REJEITADO, Instant.now(), motivo);
    }

    private void exigirPendente() {
        if (situacao != SituacaoOrcamento.PENDENTE) {
            throw new DomainException("Orçamento já foi " + situacao.name().toLowerCase());
        }
    }

    public BigDecimal getValorServicos() {
        return valorServicos;
    }

    public BigDecimal getValorPecas() {
        return valorPecas;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public Instant getGeradoEm() {
        return geradoEm;
    }

    public SituacaoOrcamento getSituacao() {
        return situacao;
    }

    public Instant getDecididoEm() {
        return decididoEm;
    }

    public String getObservacao() {
        return observacao;
    }
}
