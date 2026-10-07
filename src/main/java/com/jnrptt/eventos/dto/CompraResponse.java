package com.jnrptt.eventos.dto;

import com.jnrptt.eventos.model.Compra;
import com.jnrptt.eventos.model.EstadoCompra;
import com.jnrptt.eventos.model.TiposEntrada;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompraResponse(
        Long id,
        Long usuarioId,
        Long eventoId,
        Long tipoEntradaId,
        TiposEntrada tipoEntrada,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal total,
        EstadoCompra estado,
        LocalDateTime fecha
) {
    public static CompraResponse from(Compra compra) {
        return new CompraResponse(
                compra.getId(),
                compra.getUsuario().getId(),
                compra.getTipoEntrada().getEvento().getId(),
                compra.getTipoEntrada().getId(),
                compra.getTipoEntrada().getNombre(),
                compra.getCantidad(),
                compra.getPrecioUnitario(),
                compra.getPrecioUnitario().multiply(BigDecimal.valueOf(compra.getCantidad())),
                compra.getEstado(),
                compra.getFecha()
        );
    }
}
