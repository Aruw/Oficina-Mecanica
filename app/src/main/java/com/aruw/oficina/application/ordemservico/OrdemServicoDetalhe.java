package com.aruw.oficina.application.ordemservico;

import com.aruw.oficina.domain.cliente.Cliente;
import com.aruw.oficina.domain.estoque.TipoItemEstoque;
import com.aruw.oficina.domain.ordemservico.Orcamento;
import com.aruw.oficina.domain.ordemservico.OrdemServico;
import com.aruw.oficina.domain.ordemservico.SituacaoOrcamento;
import com.aruw.oficina.domain.ordemservico.StatusOrdemServico;
import com.aruw.oficina.domain.veiculo.Veiculo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrdemServicoDetalhe(
        UUID id,
        StatusOrdemServico status,
        String statusDescricao,
        String descricaoProblema,
        ClienteResumo cliente,
        VeiculoResumo veiculo,
        List<ItemServicoView> servicos,
        List<ItemPecaView> pecas,
        BigDecimal valorEstimado,
        OrcamentoView orcamento,
        List<HistoricoView> historico,
        Instant criadaEm,
        Instant execucaoIniciadaEm,
        Instant finalizadaEm,
        Instant entregueEm,
        Long tempoExecucaoMinutos) {

    public record ClienteResumo(UUID id, String nome, String documento) {
    }

    public record VeiculoResumo(UUID id, String placa, String marca, String modelo, int ano) {
    }

    public record ItemServicoView(UUID id, UUID servicoId, String nome, BigDecimal precoUnitario, int quantidade,
                                  BigDecimal subtotal) {
    }

    public record ItemPecaView(UUID id, UUID pecaId, String nome, TipoItemEstoque tipo, BigDecimal precoUnitario,
                               int quantidade, BigDecimal subtotal) {
    }

    public record OrcamentoView(BigDecimal valorServicos, BigDecimal valorPecas, BigDecimal valorTotal,
                                SituacaoOrcamento situacao, Instant geradoEm, Instant decididoEm, String observacao) {
    }

    public record HistoricoView(StatusOrdemServico status, Instant dataHora) {
    }

    public static OrdemServicoDetalhe de(OrdemServico os, Cliente cliente, Veiculo veiculo) {
        return new OrdemServicoDetalhe(
                os.getId(),
                os.getStatus(),
                os.getStatus().getDescricao(),
                os.getDescricaoProblema(),
                cliente == null ? null : new ClienteResumo(cliente.getId(), cliente.getNome(), cliente.getDocumento().formatado()),
                veiculo == null ? null : new VeiculoResumo(veiculo.getId(), veiculo.getPlaca().getValor(), veiculo.getMarca(),
                        veiculo.getModelo(), veiculo.getAno()),
                os.getItensServico().stream()
                        .map(i -> new ItemServicoView(i.getId(), i.getServicoId(), i.getNome(), i.getPrecoUnitario(),
                                i.getQuantidade(), i.subtotal()))
                        .toList(),
                os.getItensPeca().stream()
                        .map(i -> new ItemPecaView(i.getId(), i.getPecaId(), i.getNome(), i.getTipo(), i.getPrecoUnitario(),
                                i.getQuantidade(), i.subtotal()))
                        .toList(),
                os.calcularValorTotal(),
                os.getOrcamento().map(OrdemServicoDetalhe::orcamento).orElse(null),
                os.getHistorico().stream().map(h -> new HistoricoView(h.getStatus(), h.getDataHora())).toList(),
                os.getCriadaEm(),
                os.getExecucaoIniciadaEm(),
                os.getFinalizadaEm(),
                os.getEntregueEm(),
                os.tempoExecucao().map(d -> d.toMinutes()).orElse(null));
    }

    private static OrcamentoView orcamento(Orcamento o) {
        return new OrcamentoView(o.getValorServicos(), o.getValorPecas(), o.getValorTotal(), o.getSituacao(),
                o.getGeradoEm(), o.getDecididoEm(), o.getObservacao());
    }
}
