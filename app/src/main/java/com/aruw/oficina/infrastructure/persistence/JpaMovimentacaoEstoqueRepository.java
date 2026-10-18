package com.aruw.oficina.infrastructure.persistence;

import com.aruw.oficina.domain.estoque.MovimentacaoEstoque;
import com.aruw.oficina.domain.estoque.MovimentacaoEstoqueRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaMovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, UUID>, MovimentacaoEstoqueRepository {

}
