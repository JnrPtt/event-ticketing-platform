package com.jnrptt.eventos.repository;

import com.jnrptt.eventos.model.TipoEntrada;
import com.jnrptt.eventos.model.TiposEntrada;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoEntradaRepository extends JpaRepository<TipoEntrada, Long> {
        boolean existsByEventoIdAndNombre(Long eventoId, TiposEntrada nombre);
}
