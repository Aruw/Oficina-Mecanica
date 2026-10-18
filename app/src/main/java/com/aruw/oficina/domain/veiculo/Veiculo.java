package com.aruw.oficina.domain.veiculo;

import com.aruw.oficina.domain.shared.AggregateRoot;
import com.aruw.oficina.domain.shared.DomainException;
import com.aruw.oficina.domain.shared.Placa;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "veiculos")
public class Veiculo extends AggregateRoot {

    static final int ANO_MINIMO = 1900;
    public static final ZoneId FUSO_HORARIO = ZoneId.of("America/Sao_Paulo");

    @Id
    private UUID id;

    @Embedded
    private Placa placa;

    @Column(nullable = false, length = 60)
    private String marca;

    @Column(nullable = false, length = 80)
    private String modelo;

    @Column(nullable = false)
    private int ano;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Veiculo() {
    }

    public Veiculo(Placa placa, String marca, String modelo, int ano, UUID clienteId) {
        if (placa == null) {
            throw new DomainException("Placa do veículo é obrigatória");
        }
        this.id = UUID.randomUUID();
        this.placa = placa;
        this.criadoEm = Instant.now();
        atualizar(marca, modelo, ano, clienteId);
    }

    public void atualizar(String marca, String modelo, int ano, UUID clienteId) {
        if (marca == null || marca.isBlank()) {
            throw new DomainException("Marca do veículo é obrigatória");
        }
        if (modelo == null || modelo.isBlank()) {
            throw new DomainException("Modelo do veículo é obrigatório");
        }
        int anoMaximo = Year.now(FUSO_HORARIO).getValue() + 1;
        if (ano < ANO_MINIMO || ano > anoMaximo) {
            throw new DomainException("Ano do veículo deve estar entre " + ANO_MINIMO + " e " + anoMaximo);
        }
        if (clienteId == null) {
            throw new DomainException("Veículo deve pertencer a um cliente");
        }
        this.marca = marca.trim();
        this.modelo = modelo.trim();
        this.ano = ano;
        this.clienteId = clienteId;
    }

    public boolean pertenceA(UUID clienteId) {
        return this.clienteId.equals(clienteId);
    }

    public UUID getId() {
        return id;
    }

    public Placa getPlaca() {
        return placa;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public int getAno() {
        return ano;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
