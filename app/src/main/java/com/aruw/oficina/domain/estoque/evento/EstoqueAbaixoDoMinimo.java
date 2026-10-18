package com.aruw.oficina.domain.estoque.evento;

import com.aruw.oficina.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record EstoqueAbaixoDoMinimo(UUID pecaId, String codigo, int quantidadeAtual, int estoqueMinimo,
                                    Instant ocorridoEm) implements DomainEvent {
}
