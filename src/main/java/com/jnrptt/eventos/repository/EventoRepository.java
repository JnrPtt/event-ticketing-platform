package com.jnrptt.eventos.repository;

import com.jnrptt.eventos.model.Evento;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Long>, JpaSpecificationExecutor<Evento> {
    @EntityGraph(attributePaths = "tipoEntrada")
    Optional<Evento> findWithTipoEntradaById(Long id);

    @Override
    @EntityGraph(attributePaths = "tipoEntrada")
    List<Evento> findAll(Specification<Evento> spec);

    @Override
    @EntityGraph(attributePaths = "tipoEntrada")
    List<Evento> findAll();
}
