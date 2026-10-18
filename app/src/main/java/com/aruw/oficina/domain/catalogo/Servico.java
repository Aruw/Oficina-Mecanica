package com.aruw.oficina.domain.catalogo;

import com.aruw.oficina.domain.shared.AggregateRoot;
import com.aruw.oficina.domain.shared.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "servicos")
public class Servico extends AggregateRoot {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(name = "tempo_estimado_minutos", nullable = false)
    private int tempoEstimadoMinutos;

    @Column(nullable = false)
    private boolean ativo;

    protected Servico() {
    }

    public Servico(String nome, String descricao, BigDecimal preco, int tempoEstimadoMinutos) {
        this.id = UUID.randomUUID();
        this.ativo = true;
        atualizar(nome, descricao, preco, tempoEstimadoMinutos);
    }

    public void atualizar(String nome, String descricao, BigDecimal preco, int tempoEstimadoMinutos) {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("Nome do serviço é obrigatório");
        }
        if (preco == null || preco.signum() < 0) {
            throw new DomainException("Preço do serviço deve ser maior ou igual a zero");
        }
        if (tempoEstimadoMinutos <= 0) {
            throw new DomainException("Tempo estimado do serviço deve ser positivo");
        }
        this.nome = nome.trim();
        this.descricao = descricao;
        this.preco = preco.setScale(2, RoundingMode.HALF_EVEN);
        this.tempoEstimadoMinutos = tempoEstimadoMinutos;
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getTempoEstimadoMinutos() {
        return tempoEstimadoMinutos;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
