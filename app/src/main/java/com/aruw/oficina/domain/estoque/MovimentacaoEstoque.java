package com.aruw.oficina.domain.estoque;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movimentacoes_estoque")
public class MovimentacaoEstoque {

    public enum Tipo { ENTRADA, SAIDA }

    @Id
    private UUID id;

    @Column(name = "peca_id", nullable = false)
    private UUID pecaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Tipo tipo;

    @Column(nullable = false)
    private int quantidade;

    @Column(length = 255)
    private String motivo;

    @Column(name = "ordem_servico_id")
    private UUID ordemServicoId;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    protected MovimentacaoEstoque() {
    }

    private MovimentacaoEstoque(UUID pecaId, Tipo tipo, int quantidade, String motivo, UUID ordemServicoId) {
        this.id = UUID.randomUUID();
        this.pecaId = pecaId;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.motivo = motivo;
        this.ordemServicoId = ordemServicoId;
        this.dataHora = Instant.now();
    }

    public static MovimentacaoEstoque entrada(UUID pecaId, int quantidade, String motivo) {
        return new MovimentacaoEstoque(pecaId, Tipo.ENTRADA, quantidade, motivo, null);
    }

    public static MovimentacaoEstoque saidaPorOrdemServico(UUID pecaId, int quantidade, UUID ordemServicoId) {
        return new MovimentacaoEstoque(pecaId, Tipo.SAIDA, quantidade, "Baixa por aprovação de orçamento", ordemServicoId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPecaId() {
        return pecaId;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public String getMotivo() {
        return motivo;
    }

    public UUID getOrdemServicoId() {
        return ordemServicoId;
    }

    public Instant getDataHora() {
        return dataHora;
    }
}
