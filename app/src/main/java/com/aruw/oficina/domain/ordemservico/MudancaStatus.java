package com.aruw.oficina.domain.ordemservico;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.Instant;

@Embeddable
public class MudancaStatus {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private StatusOrdemServico status;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora;

    protected MudancaStatus() {
    }

    MudancaStatus(StatusOrdemServico status, Instant dataHora) {
        this.status = status;
        this.dataHora = dataHora;
    }

    public StatusOrdemServico getStatus() {
        return status;
    }

    public Instant getDataHora() {
        return dataHora;
    }
}
