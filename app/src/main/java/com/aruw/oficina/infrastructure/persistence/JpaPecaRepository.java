package com.aruw.oficina.infrastructure.persistence;

import com.aruw.oficina.domain.estoque.Peca;
import com.aruw.oficina.domain.estoque.PecaRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaPecaRepository extends JpaRepository<Peca, UUID>, PecaRepository {

}
