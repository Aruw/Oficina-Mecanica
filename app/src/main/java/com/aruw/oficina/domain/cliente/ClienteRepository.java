package com.aruw.oficina.domain.cliente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepository {

    Cliente save(Cliente cliente);

    Optional<Cliente> findById(UUID id);

    Optional<Cliente> findByDocumentoValor(String documento);

    boolean existsByDocumentoValor(String documento);

    List<Cliente> findAll();

    void delete(Cliente cliente);
}
