package com.jnrptt.eventos.repository;

import com.jnrptt.eventos.model.Compra;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CompraRepository extends JpaRepository<Compra, Long> {
    boolean existsByTipoEntradaId(Long tipoEntradaId);

    boolean existsByTipoEntradaEventoId(Long eventoId);

    @EntityGraph(attributePaths = "tipoEntrada")
    Page<Compra> findByUsuarioId(Long usuarioId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Compra c where c.id = :id and c.usuario.id = :usuarioId")
    Optional<Compra> findByIdAndUsuarioIdForUpdate(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
