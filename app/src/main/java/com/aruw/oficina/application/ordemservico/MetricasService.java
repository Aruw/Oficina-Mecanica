package com.aruw.oficina.application.ordemservico;

import com.aruw.oficina.domain.catalogo.Servico;
import com.aruw.oficina.domain.catalogo.ServicoRepository;
import com.aruw.oficina.domain.ordemservico.ItemServico;
import com.aruw.oficina.domain.ordemservico.OrdemServico;
import com.aruw.oficina.domain.ordemservico.OrdemServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MetricasService {

    private final OrdemServicoRepository ordens;
    private final ServicoRepository servicos;

    public MetricasService(OrdemServicoRepository ordens, ServicoRepository servicos) {
        this.ordens = ordens;
        this.servicos = servicos;
    }

    @Transactional(readOnly = true)
    public TempoMedioExecucao tempoMedioExecucao() {
        List<OrdemServico> executadas = ordens.findByExecucaoIniciadaEmIsNotNullAndFinalizadaEmIsNotNull();
        if (executadas.isEmpty()) {
            return new TempoMedioExecucao(0, null, List.of());
        }
        double mediaGeral = executadas.stream()
                .mapToLong(os -> minutos(os))
                .average()
                .orElse(0);

        Map<UUID, List<Long>> temposPorServico = new LinkedHashMap<>();
        Map<UUID, String> nomes = new LinkedHashMap<>();
        for (OrdemServico os : executadas) {
            long minutos = minutos(os);
            for (ItemServico item : os.getItensServico()) {
                temposPorServico.computeIfAbsent(item.getServicoId(), k -> new ArrayList<>()).add(minutos);
                nomes.putIfAbsent(item.getServicoId(), item.getNome());
            }
        }

        List<TempoMedioExecucao.PorServico> porServico = temposPorServico.entrySet().stream()
                .map(e -> new TempoMedioExecucao.PorServico(
                        e.getKey(),
                        nomes.get(e.getKey()),
                        e.getValue().size(),
                        e.getValue().stream().mapToLong(Long::longValue).average().orElse(0),
                        servicos.findById(e.getKey()).map(Servico::getTempoEstimadoMinutos).orElse(null)))
                .sorted(Comparator.comparing(TempoMedioExecucao.PorServico::nome))
                .toList();

        return new TempoMedioExecucao(executadas.size(), mediaGeral, porServico);
    }

    private static long minutos(OrdemServico os) {
        return os.tempoExecucao().map(Duration::toMinutes).orElse(0L);
    }
}
