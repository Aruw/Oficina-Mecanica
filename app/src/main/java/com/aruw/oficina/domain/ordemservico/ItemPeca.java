package com.aruw.oficina.domain.ordemservico;

import com.aruw.oficina.domain.estoque.TipoItemEstoque;
import com.aruw.oficina.domain.shared.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "os_itens_peca")
public class ItemPeca {

    @Id
    private UUID id;

    @Column(name = "peca_id", nullable = false)
    private UUID pecaId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoItemEstoque tipo;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private int quantidade;

    protected ItemPeca() {
    }

    ItemPeca(UUID pecaId, String nome, TipoItemEstoque tipo, BigDecimal precoUnitario, int quantidade) {
        if (quantidade <= 0) {
            throw new DomainException("Quantidade da peça/insumo deve ser maior que zero");
        }
        this.id = UUID.randomUUID();
        this.pecaId = pecaId;
        this.nome = nome;
        this.tipo = tipo;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
    }

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public UUID getId() {
        return id;
    }

    public UUID getPecaId() {
        return pecaId;
    }

    public String getNome() {
        return nome;
    }

    public TipoItemEstoque getTipo() {
        return tipo;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public int getQuantidade() {
        return quantidade;
    }
}
