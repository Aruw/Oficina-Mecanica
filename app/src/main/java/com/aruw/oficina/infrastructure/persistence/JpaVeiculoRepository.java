package com.aruw.oficina.infrastructure.persistence;

import com.aruw.oficina.domain.veiculo.Veiculo;
import com.aruw.oficina.domain.veiculo.VeiculoRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaVeiculoRepository extends JpaRepository<Veiculo, UUID>, VeiculoRepository {

}
