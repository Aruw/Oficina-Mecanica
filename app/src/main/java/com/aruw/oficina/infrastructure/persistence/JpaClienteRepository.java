package com.aruw.oficina.infrastructure.persistence;

import com.aruw.oficina.domain.cliente.Cliente;
import com.aruw.oficina.domain.cliente.ClienteRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Adaptador Spring Data JPA da porta {@link ClienteRepository} do domínio.
 */
public interface JpaClienteRepository extends JpaRepository<Cliente, UUID>, ClienteRepository {
}
