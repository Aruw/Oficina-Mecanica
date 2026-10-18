package com.aruw.oficina.application.ordemservico;

import com.aruw.oficina.domain.catalogo.Servico;
import com.aruw.oficina.domain.catalogo.ServicoRepository;
import com.aruw.oficina.domain.ordemservico.OrdemServico;
import com.aruw.oficina.domain.ordemservico.OrdemServicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricasServiceTest {

    @Mock OrdemServicoRepository ordens;
    @Mock ServicoRepository servicos;
    @InjectMocks MetricasService service;

    @Test
    void semOrdensExecutadas() {
        when(ordens.findByExecucaoIniciadaEmIsNotNullAndFinalizadaEmIsNotNull()).thenReturn(List.of());
        TempoMedioExecucao resultado = service.tempoMedioExecucao();
        assertThat(resultado.ordensConsideradas()).isZero();
        assertThat(resultado.mediaGeralMinutos()).isNull();
        assertThat(resultado.porServico()).isEmpty();
    }

    @Test
    void calculaMediaGeralEPorServico() {
        Servico servico = new Servico("Alinhamento", null, BigDecimal.TEN, 60);
        OrdemServico os = OrdemServico.abrir(UUID.randomUUID(), UUID.randomUUID(), null);
        os.adicionarServico(servico.getId(), servico.getNome(), servico.getPreco(), 1);
        os.iniciarDiagnostico();
        os.gerarOrcamento();
        os.aprovarOrcamento();
        os.finalizar();
        when(ordens.findByExecucaoIniciadaEmIsNotNullAndFinalizadaEmIsNotNull()).thenReturn(List.of(os));
        when(servicos.findById(any())).thenReturn(Optional.of(servico));

        TempoMedioExecucao resultado = service.tempoMedioExecucao();

        assertThat(resultado.ordensConsideradas()).isEqualTo(1);
        assertThat(resultado.mediaGeralMinutos()).isZero();
        assertThat(resultado.porServico()).singleElement().satisfies(p -> {
            assertThat(p.nome()).isEqualTo("Alinhamento");
            assertThat(p.tempoEstimadoMinutos()).isEqualTo(60);
            assertThat(p.ordensConsideradas()).isEqualTo(1);
            assertThat(p.servicoId()).isEqualTo(servico.getId());
            assertThat(p.mediaMinutos()).isZero();
        });
    }
}
