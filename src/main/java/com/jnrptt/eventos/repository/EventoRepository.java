package com.jnrptt.eventos.repository;

import com.jnrptt.eventos.model.Evento;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EventoRepository extends JpaRepository<Evento, Long> {
    @EntityGraph(attributePaths = "tipoEntrada")
    Optional<Evento> findWithTipoEntradaById(Long id);

    @Override
    @EntityGraph(attributePaths = "tipoEntrada")
    List<Evento> findAll();
}
