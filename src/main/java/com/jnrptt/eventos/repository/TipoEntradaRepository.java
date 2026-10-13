package com.jnrptt.eventos.repository;

import com.jnrptt.eventos.model.TipoEntrada;
import com.jnrptt.eventos.model.TiposEntrada;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TipoEntradaRepository extends JpaRepository<TipoEntrada, Long> {
        boolean existsByEventoIdAndNombre(Long eventoId, TiposEntrada nombre);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select t from TipoEntrada t where t.id = :id")
        Optional<TipoEntrada> findByIdForUpdate(@Param("id") Long id);
}
