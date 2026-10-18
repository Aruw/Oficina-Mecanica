package com.aruw.oficina.domain.estoque;

import java.util.List;
import java.util.UUID;

public interface MovimentacaoEstoqueRepository {

    MovimentacaoEstoque save(MovimentacaoEstoque movimentacao);

    List<MovimentacaoEstoque> findByPecaIdOrderByDataHoraDesc(UUID pecaId);
}
