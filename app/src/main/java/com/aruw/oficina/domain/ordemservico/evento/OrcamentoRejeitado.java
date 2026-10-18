package com.aruw.oficina.domain.ordemservico.evento;

import com.aruw.oficina.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrcamentoRejeitado(UUID ordemServicoId, UUID clienteId, String motivo, Instant ocorridoEm)
        implements DomainEvent {

}
