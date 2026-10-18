package com.aruw.oficina.domain.cliente;

import com.aruw.oficina.domain.shared.AggregateRoot;
import com.aruw.oficina.domain.shared.Documento;
import com.aruw.oficina.domain.shared.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clientes")
public class Cliente extends AggregateRoot {

    @Id
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Embedded
    private Documento documento;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Cliente() {
    }

    public Cliente(String nome, Documento documento, String email, String telefone) {
        if (documento == null) {
            throw new DomainException("Documento do cliente é obrigatório");
        }
        this.id = UUID.randomUUID();
        this.documento = documento;
        this.criadoEm = Instant.now();
        atualizar(nome, email, telefone);
    }

    public void atualizar(String nome, String email, String telefone) {
        if (nome == null || nome.isBlank()) {
            throw new DomainException("Nome do cliente é obrigatório");
        }
        this.nome = nome.trim();
        this.email = email;
        this.telefone = telefone;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Documento getDocumento() {
        return documento;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
