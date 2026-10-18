package com.aruw.oficina.application.estoque;

import com.aruw.oficina.domain.estoque.TipoItemEstoque;

import java.math.BigDecimal;

public record PecaCommand(String codigo, String nome, String descricao, TipoItemEstoque tipo,
                          BigDecimal precoUnitario, int quantidadeEstoque, int estoqueMinimo) {
}
