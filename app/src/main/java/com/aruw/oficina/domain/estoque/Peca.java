package com.aruw.oficina.domain.estoque;

import com.aruw.oficina.domain.estoque.evento.EstoqueAbaixoDoMinimo;
import com.aruw.oficina.domain.shared.AggregateRoot;
import com.aruw.oficina.domain.shared.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pecas")
public class Peca extends AggregateRoot {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoItemEstoque tipo;

    @Column(name = "preco_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "quantidade_estoque", nullable = false)
    private int quantidadeEstoque;

    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;

    protected Peca() {
    }

    public Peca(String codigo, String nome, String descricao, TipoItemEstoque tipo,
                BigDecimal precoUnitario, int quantidadeInicial, int estoqueMinimo) {
        if (codigo == null || codigo.isBlank()) {
            throw new DomainException("Código da peça/insumo é obrigatório");
        }
        if (quantidadeInicial < 0) {
            throw new DomainException("Quantidade inicial não pode ser negativa");
        }
        this.id = UUID.randomUUID();
        this.codigo = codigo.trim().toUpperCase();
        this.quantidadeEstoque = quantidadeInicial;
        atualizar(nome, descricao, tipo, precoUnitario, estoqueMinimo);
    }

    public void atualizar(String nome, String descricao, TipoItemEstoque tipo, BigDecimal precoUnitario, int estoqueMinimo) {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("Nome da peça/insumo é obrigatório");
        }
        if (tipo == null) {
            throw new DomainException("Tipo (PECA ou INSUMO) é obrigatório");
        }
        if (precoUnitario == null || precoUnitario.signum() < 0) {
            throw new DomainException("Preço unitário deve ser maior ou igual a zero");
        }
        if (estoqueMinimo < 0) {
            throw new DomainException("Estoque mínimo não pode ser negativo");
        }
        this.nome = nome.trim();
        this.descricao = descricao;
        this.tipo = tipo;
        this.precoUnitario = precoUnitario.setScale(2, RoundingMode.HALF_EVEN);
        this.estoqueMinimo = estoqueMinimo;
    }

    public void darEntrada(int quantidade) {
        validarQuantidade(quantidade);
        this.quantidadeEstoque += quantidade;
    }

    public void darBaixa(int quantidade) {
        validarQuantidade(quantidade);
        if (!possuiDisponivel(quantidade)) {
            throw new EstoqueInsuficienteException(codigo, quantidadeEstoque, quantidade);
        }
        this.quantidadeEstoque -= quantidade;
        if (abaixoDoMinimo()) {
            registrarEvento(new EstoqueAbaixoDoMinimo(id, codigo, quantidadeEstoque, estoqueMinimo, Instant.now()));
        }
    }

    public boolean possuiDisponivel(int quantidade) {
        return quantidadeEstoque >= quantidade;
    }

    public boolean abaixoDoMinimo() {
        return quantidadeEstoque <= estoqueMinimo;
    }

    private static void validarQuantidade(int quantidade) {
        if (quantidade <= 0) {
            throw new DomainException("Quantidade deve ser maior que zero");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public TipoItemEstoque getTipo() {
        return tipo;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public int getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }
}
