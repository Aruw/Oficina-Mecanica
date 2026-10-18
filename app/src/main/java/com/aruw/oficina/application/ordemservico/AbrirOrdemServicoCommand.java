package com.aruw.oficina.application.ordemservico;

import java.util.List;
import java.util.UUID;

public record AbrirOrdemServicoCommand(
        Cliente cliente,
        Veiculo veiculo,
        String descricaoProblema,
        List<ItemServico> servicos,
        List<ItemPeca> pecas) {

    public record Cliente(String documento, String nome, String email, String telefone) {
    }

    public record Veiculo(String placa, String marca, String modelo, Integer ano) {
    }

    public record ItemServico(UUID servicoId, int quantidade) {
    }

    public record ItemPeca(UUID pecaId, int quantidade) {
    }

    public List<ItemServico> servicosOuVazio() {
        return servicos == null ? List.of() : servicos;
    }

    public List<ItemPeca> pecasOuVazio() {
        return pecas == null ? List.of() : pecas;
    }
}
