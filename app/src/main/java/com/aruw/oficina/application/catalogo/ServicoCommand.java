package com.aruw.oficina.application.catalogo;

import java.math.BigDecimal;

public record ServicoCommand(String nome, String descricao, BigDecimal preco, int tempoEstimadoMinutos, Boolean ativo) {
}
