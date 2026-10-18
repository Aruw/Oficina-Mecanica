package com.aruw.oficina.infrastructure.persistence;

import com.aruw.oficina.domain.catalogo.Servico;
import com.aruw.oficina.domain.catalogo.ServicoRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaServicoRepository extends JpaRepository<Servico, UUID>, ServicoRepository {

}
