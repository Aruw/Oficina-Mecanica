package com.aruw.oficina.domain.estoque;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PecaRepository {

    Peca save(Peca peca);

    Optional<Peca> findById(UUID id);

    boolean existsByCodigo(String codigo);

    List<Peca> findAll();

    void delete(Peca peca);
}
