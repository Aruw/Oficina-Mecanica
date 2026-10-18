package com.aruw.oficina.domain.ordemservico.evento;

import com.aruw.oficina.domain.shared.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrcamentoAprovado(UUID ordemServicoId, UUID clienteId, BigDecimal valorTotal, Instant ocorridoEm)
        implements DomainEvent {

}
