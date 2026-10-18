package com.aruw.oficina.domain.ordemservico.evento;

import com.aruw.oficina.domain.ordemservico.StatusOrdemServico;
import com.aruw.oficina.domain.shared.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record StatusOrdemServicoAlterado(UUID ordemServicoId, StatusOrdemServico anterior, StatusOrdemServico novo,
                                         Instant ocorridoEm) implements DomainEvent {

}
