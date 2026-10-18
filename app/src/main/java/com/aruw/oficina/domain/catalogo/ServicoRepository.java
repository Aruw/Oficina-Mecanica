package com.aruw.oficina.domain.catalogo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServicoRepository {

    Servico save(Servico servico);

    Optional<Servico> findById(UUID id);

    List<Servico> findAll();

    void delete(Servico servico);
}
