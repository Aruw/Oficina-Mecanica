package com.aruw.oficina.infrastructure.persistence;

import com.aruw.oficina.domain.ordemservico.OrdemServico;
import com.aruw.oficina.domain.ordemservico.OrdemServicoRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaOrdemServicoRepository extends JpaRepository<OrdemServico, UUID>, OrdemServicoRepository {

}
