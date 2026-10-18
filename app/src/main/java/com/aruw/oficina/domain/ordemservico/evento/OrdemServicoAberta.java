package com.aruw.oficina.domain.ordemservico.evento;

import com.aruw.oficina.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record OrdemServicoAberta(UUID ordemServicoId, UUID clienteId, UUID veiculoId, Instant ocorridoEm)
        implements DomainEvent {

}
