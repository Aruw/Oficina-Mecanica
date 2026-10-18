package com.aruw.oficina.domain.ordemservico;

import com.aruw.oficina.domain.shared.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "os_itens_servico")
public class ItemServico {

    @Id
    private UUID id;

    @Column(name = "servico_id", nullable = false)
    private UUID servicoId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private int quantidade;

    protected ItemServico() {
    }

    ItemServico(UUID servicoId, String nome, BigDecimal precoUnitario, int quantidade) {
        if (quantidade <= 0) {
            throw new DomainException("Quantidade do serviço deve ser maior que zero");
        }
        this.id = UUID.randomUUID();
        this.servicoId = servicoId;
        this.nome = nome;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
    }

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public UUID getId() {
        return id;
    }

    public UUID getServicoId() {
        return servicoId;
    }

    public String getNome() {
        return nome;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public int getQuantidade() {
        return quantidade;
    }
}
