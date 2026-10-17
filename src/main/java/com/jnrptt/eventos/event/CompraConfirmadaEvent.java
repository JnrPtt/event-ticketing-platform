package com.jnrptt.eventos.event;

import com.jnrptt.eventos.model.Compra;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompraConfirmadaEvent(
        Long compraId,
        Long usuarioId,
        String email,
        String nombreEvento,
        String tipoEntrada,
        Integer cantidad,
        BigDecimal total,
        LocalDateTime fechaCompra
) {
    public static CompraConfirmadaEvent from(Compra compra) {
        return new CompraConfirmadaEvent(
                compra.getId(),
                compra.getUsuario().getId(),
                compra.getUsuario().getEmail(),
                compra.getTipoEntrada().getEvento().getNombre(),
                compra.getTipoEntrada().getNombre().name(),
                compra.getCantidad(),
                compra.getPrecioUnitario().multiply(BigDecimal.valueOf(compra.getCantidad())),
                compra.getFecha()
        );
    }
}
