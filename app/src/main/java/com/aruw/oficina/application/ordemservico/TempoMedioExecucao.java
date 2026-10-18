package com.aruw.oficina.application.ordemservico;

import java.util.List;
import java.util.UUID;

public record TempoMedioExecucao(long ordensConsideradas, Double mediaGeralMinutos, List<PorServico> porServico) {

    public record PorServico(UUID servicoId, String nome, long ordensConsideradas, double mediaMinutos,
                             Integer tempoEstimadoMinutos) {
    }
}
