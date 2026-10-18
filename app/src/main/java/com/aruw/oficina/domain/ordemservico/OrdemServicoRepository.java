package com.aruw.oficina.domain.ordemservico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrdemServicoRepository {

    OrdemServico save(OrdemServico ordemServico);

    Optional<OrdemServico> findById(UUID id);

    List<OrdemServico> findAll();

    List<OrdemServico> findByStatus(StatusOrdemServico status);

    boolean existsByClienteId(UUID clienteId);

    boolean existsByVeiculoId(UUID veiculoId);

    List<OrdemServico> findByExecucaoIniciadaEmIsNotNullAndFinalizadaEmIsNotNull();
}
