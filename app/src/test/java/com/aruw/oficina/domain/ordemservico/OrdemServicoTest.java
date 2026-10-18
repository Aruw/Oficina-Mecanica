package com.aruw.oficina.domain.ordemservico;

import com.aruw.oficina.domain.estoque.TipoItemEstoque;
import com.aruw.oficina.domain.ordemservico.evento.OrcamentoAprovado;
import com.aruw.oficina.domain.ordemservico.evento.OrcamentoGerado;
import com.aruw.oficina.domain.ordemservico.evento.OrcamentoRejeitado;
import com.aruw.oficina.domain.ordemservico.evento.OrdemServicoAberta;
import com.aruw.oficina.domain.ordemservico.evento.StatusOrdemServicoAlterado;
import com.aruw.oficina.domain.shared.DomainEvent;
import com.aruw.oficina.domain.shared.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdemServicoTest {

    private final UUID clienteId = UUID.randomUUID();
    private final UUID veiculoId = UUID.randomUUID();

    private OrdemServico osComItens() {
        OrdemServico os = OrdemServico.abrir(clienteId, veiculoId, "Barulho no motor");
        os.adicionarServico(UUID.randomUUID(), "Troca de óleo", new BigDecimal("80.00"), 1);
        os.adicionarPeca(UUID.randomUUID(), "Óleo 5W30", TipoItemEstoque.INSUMO, new BigDecimal("45.00"), 4);
        return os;
    }

    private OrdemServico osAguardandoAprovacao() {
        OrdemServico os = osComItens();
        os.iniciarDiagnostico();
        os.gerarOrcamento();
        os.puxarEventos();
        return os;
    }

    @Test
    void abreOsComoRecebidaERegistraEvento() {
        OrdemServico os = OrdemServico.abrir(clienteId, veiculoId, "Revisão");
        assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.RECEBIDA);
        assertThat(os.getHistorico()).extracting(MudancaStatus::getStatus).containsExactly(StatusOrdemServico.RECEBIDA);
        assertThat(os.getHistorico().getFirst().getDataHora()).isNotNull();
        assertThat(os.puxarEventos()).singleElement().isInstanceOf(OrdemServicoAberta.class);
        assertThat(os.pertenceAoCliente(clienteId)).isTrue();
        assertThat(os.getDescricaoProblema()).isEqualTo("Revisão");
        assertThat(os.getVeiculoId()).isEqualTo(veiculoId);
        assertThat(os.getOrcamento()).isEmpty();
        assertThat(os.tempoExecucao()).isEmpty();
    }

    @Test
    void exigeClienteEVeiculo() {
        assertThatThrownBy(() -> OrdemServico.abrir(null, veiculoId, null)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> OrdemServico.abrir(clienteId, null, null)).isInstanceOf(DomainException.class);
    }

    @Test
    void calculaValoresDosItens() {
        OrdemServico os = osComItens();
        os.adicionarServico(UUID.randomUUID(), "Alinhamento", new BigDecimal("120.00"), 2);
        assertThat(os.calcularValorServicos()).isEqualByComparingTo("320.00");
        assertThat(os.calcularValorPecas()).isEqualByComparingTo("180.00");
        assertThat(os.calcularValorTotal()).isEqualByComparingTo("500.00");
        ItemServico item = os.getItensServico().getLast();
        assertThat(item.getNome()).isEqualTo("Alinhamento");
        assertThat(item.getQuantidade()).isEqualTo(2);
        assertThat(item.getPrecoUnitario()).isEqualByComparingTo("120");
        assertThat(item.getServicoId()).isNotNull();
        ItemPeca peca = os.getItensPeca().getFirst();
        assertThat(peca.getTipo()).isEqualTo(TipoItemEstoque.INSUMO);
        assertThat(peca.getNome()).isEqualTo("Óleo 5W30");
        assertThat(peca.getPrecoUnitario()).isEqualByComparingTo("45");
    }

    @Test
    void quantidadeDosItensDeveSerPositiva() {
        OrdemServico os = OrdemServico.abrir(clienteId, veiculoId, null);
        assertThatThrownBy(() -> os.adicionarServico(UUID.randomUUID(), "x", BigDecimal.ONE, 0)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> os.adicionarPeca(UUID.randomUUID(), "x", TipoItemEstoque.PECA, BigDecimal.ONE, 0))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void removeItens() {
        OrdemServico os = osComItens();
        os.removerItemServico(os.getItensServico().getFirst().getId());
        os.removerItemPeca(os.getItensPeca().getFirst().getId());
        assertThat(os.getItensServico()).isEmpty();
        assertThat(os.getItensPeca()).isEmpty();
        UUID inexistente = UUID.randomUUID();
        assertThatThrownBy(() -> os.removerItemServico(inexistente)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> os.removerItemPeca(inexistente)).isInstanceOf(DomainException.class);
    }

    @Test
    void fluxoCompletoComAprovacao() {
        OrdemServico os = osComItens();
        os.puxarEventos();

        os.iniciarDiagnostico();
        assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.EM_DIAGNOSTICO);
        assertThat(os.getDiagnosticoIniciadoEm()).isNotNull();

        Orcamento orcamento = os.gerarOrcamento();
        assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.AGUARDANDO_APROVACAO);
        assertThat(orcamento.getValorServicos()).isEqualByComparingTo("80.00");
        assertThat(orcamento.getValorPecas()).isEqualByComparingTo("180.00");
        assertThat(orcamento.getValorTotal()).isEqualByComparingTo("260.00");
        assertThat(orcamento.getSituacao()).isEqualTo(SituacaoOrcamento.PENDENTE);
        assertThat(orcamento.getGeradoEm()).isNotNull();

        os.aprovarOrcamento();
        assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.EM_EXECUCAO);
        assertThat(os.getOrcamento()).get().extracting(Orcamento::getSituacao).isEqualTo(SituacaoOrcamento.APROVADO);
        assertThat(os.getOrcamento().get().getDecididoEm()).isNotNull();
        assertThat(os.getExecucaoIniciadaEm()).isNotNull();

        os.finalizar();
        assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.FINALIZADA);
        assertThat(os.tempoExecucao()).isPresent();

        os.entregar();
        assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.ENTREGUE);
        assertThat(os.getEntregueEm()).isNotNull();
        assertThat(os.getFinalizadaEm()).isNotNull();

        assertThat(os.getHistorico()).extracting(MudancaStatus::getStatus).containsExactly(
                StatusOrdemServico.RECEBIDA, StatusOrdemServico.EM_DIAGNOSTICO, StatusOrdemServico.AGUARDANDO_APROVACAO,
                StatusOrdemServico.EM_EXECUCAO, StatusOrdemServico.FINALIZADA, StatusOrdemServico.ENTREGUE);
        assertThat(os.puxarEventos()).extracting(DomainEvent::getClass).contains(
                StatusOrdemServicoAlterado.class, OrcamentoGerado.class, OrcamentoAprovado.class);
    }

    @Test
    void rejeicaoEncerraOsSemExecucao() {
        OrdemServico os = osAguardandoAprovacao();
        os.rejeitarOrcamento("Valor alto");
        assertThat(os.getStatus()).isEqualTo(StatusOrdemServico.FINALIZADA);
        assertThat(os.getOrcamento().get().getSituacao()).isEqualTo(SituacaoOrcamento.REJEITADO);
        assertThat(os.getOrcamento().get().getObservacao()).isEqualTo("Valor alto");
        assertThat(os.tempoExecucao()).isEmpty();
        assertThat(os.puxarEventos()).extracting(DomainEvent::getClass).contains(OrcamentoRejeitado.class);
    }

    @Test
    void naoGeraOrcamentoSemItens() {
        OrdemServico os = OrdemServico.abrir(clienteId, veiculoId, null);
        os.iniciarDiagnostico();
        assertThatThrownBy(os::gerarOrcamento).isInstanceOf(DomainException.class).hasMessageContaining("ao menos um");
    }

    @Test
    void naoGeraOrcamentoAntesDoDiagnostico() {
        OrdemServico os = osComItens();
        assertThatThrownBy(os::gerarOrcamento).isInstanceOf(TransicaoStatusInvalidaException.class);
    }

    @Test
    void naoAprovaSemOrcamentoPendente() {
        OrdemServico os = osComItens();
        assertThatThrownBy(os::aprovarOrcamento).isInstanceOf(TransicaoStatusInvalidaException.class);
        assertThatThrownBy(() -> os.rejeitarOrcamento("x")).isInstanceOf(TransicaoStatusInvalidaException.class);
    }

    @Test
    void naoPermiteAlterarItensAposEnvioDoOrcamento() {
        OrdemServico os = osAguardandoAprovacao();
        UUID itemId = os.getItensServico().getFirst().getId();
        assertThatThrownBy(() -> os.adicionarServico(UUID.randomUUID(), "x", BigDecimal.ONE, 1)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> os.adicionarPeca(UUID.randomUUID(), "x", TipoItemEstoque.PECA, BigDecimal.ONE, 1))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> os.removerItemServico(itemId)).isInstanceOf(DomainException.class);
    }

    @Test
    void naoPulaEtapas() {
        OrdemServico os = osComItens();
        assertThatThrownBy(os::finalizar).isInstanceOf(TransicaoStatusInvalidaException.class)
                .hasMessageContaining("RECEBIDA");
        assertThatThrownBy(os::entregar).isInstanceOf(TransicaoStatusInvalidaException.class);
        os.iniciarDiagnostico();
        assertThatThrownBy(os::iniciarDiagnostico).isInstanceOf(TransicaoStatusInvalidaException.class);
    }

    @Test
    void orcamentoSoPodeSerDecididoUmaVez() {
        Orcamento aprovado = Orcamento.gerar(BigDecimal.TEN, BigDecimal.ONE).aprovar();
        assertThatThrownBy(aprovado::aprovar).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> aprovado.rejeitar("x")).isInstanceOf(DomainException.class);
        assertThat(aprovado.getValorTotal()).isEqualByComparingTo("11");
    }

    @ParameterizedTest
    @EnumSource(StatusOrdemServico.class)
    void statusPossuemDescricaoEPrioridade(StatusOrdemServico status) {
        assertThat(status.getDescricao()).isNotBlank();
        assertThat(status.prioridade()).isBetween(1, 6);
        assertThat(status.isEmAndamento()).isEqualTo(status != StatusOrdemServico.FINALIZADA && status != StatusOrdemServico.ENTREGUE);
        assertThat(status.proximosPermitidos()).doesNotContain(status);
    }

    @Test
    void transicoesPermitidas() {
        assertThat(StatusOrdemServico.AGUARDANDO_APROVACAO.proximosPermitidos())
                .containsExactlyInAnyOrder(StatusOrdemServico.EM_EXECUCAO, StatusOrdemServico.FINALIZADA);
        assertThat(StatusOrdemServico.ENTREGUE.proximosPermitidos()).isEmpty();
        assertThat(StatusOrdemServico.RECEBIDA.podeTransitarPara(StatusOrdemServico.EM_EXECUCAO)).isFalse();
    }
}
