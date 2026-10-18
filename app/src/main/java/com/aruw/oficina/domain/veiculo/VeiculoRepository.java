package com.aruw.oficina.domain.veiculo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VeiculoRepository {

    Veiculo save(Veiculo veiculo);

    Optional<Veiculo> findById(UUID id);

    Optional<Veiculo> findByPlacaValor(String placa);

    boolean existsByPlacaValor(String placa);

    boolean existsByClienteId(UUID clienteId);

    List<Veiculo> findByClienteId(UUID clienteId);

    List<Veiculo> findAll();

    void delete(Veiculo veiculo);
}
