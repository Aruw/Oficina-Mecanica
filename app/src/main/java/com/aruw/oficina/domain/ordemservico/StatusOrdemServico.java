package com.aruw.oficina.domain.ordemservico;

import java.util.EnumSet;
import java.util.Set;

public enum StatusOrdemServico {

    RECEBIDA("Recebida"),
    EM_DIAGNOSTICO("Em diagnóstico"),
    AGUARDANDO_APROVACAO("Aguardando aprovação"),
    EM_EXECUCAO("Em execução"),
    FINALIZADA("Finalizada"),
    ENTREGUE("Entregue");

    private final String descricao;

    StatusOrdemServico(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public Set<StatusOrdemServico> proximosPermitidos() {
        return switch (this) {
            case RECEBIDA -> EnumSet.of(EM_DIAGNOSTICO);
            case EM_DIAGNOSTICO -> EnumSet.of(AGUARDANDO_APROVACAO);
            case AGUARDANDO_APROVACAO -> EnumSet.of(EM_EXECUCAO, FINALIZADA);
            case EM_EXECUCAO -> EnumSet.of(FINALIZADA);
            case FINALIZADA -> EnumSet.of(ENTREGUE);
            case ENTREGUE -> EnumSet.noneOf(StatusOrdemServico.class);
        };
    }

    public boolean podeTransitarPara(StatusOrdemServico destino) {
        return proximosPermitidos().contains(destino);
    }

    public boolean permiteAlterarItens() {
        return this == RECEBIDA || this == EM_DIAGNOSTICO;
    }

    public boolean isEmAndamento() {
        return this != FINALIZADA && this != ENTREGUE;
    }

    public int prioridade() {
        return switch (this) {
            case EM_EXECUCAO -> 1;
            case AGUARDANDO_APROVACAO -> 2;
            case EM_DIAGNOSTICO -> 3;
            case RECEBIDA -> 4;
            case FINALIZADA -> 5;
            case ENTREGUE -> 6;
        };
    }
}
