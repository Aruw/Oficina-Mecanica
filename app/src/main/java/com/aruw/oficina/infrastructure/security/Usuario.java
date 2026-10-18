package com.aruw.oficina.infrastructure.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "usuarios")
public class Usuario {

    public enum Perfil { ADMIN, ATENDENTE }

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Column(nullable = false)
    private boolean ativo;

    protected Usuario() {
    }

    public Usuario(String username, String senhaHash, Perfil perfil) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.ativo = true;
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
