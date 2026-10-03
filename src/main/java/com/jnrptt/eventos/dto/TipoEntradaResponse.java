package com.jnrptt.eventos.dto;

import com.jnrptt.eventos.model.TiposEntrada;
import com.jnrptt.eventos.model.TipoEntrada;

import java.math.BigDecimal;

public record TipoEntradaResponse(
        Long id,
        TiposEntrada nombre,
        BigDecimal precio,
        Integer aforoTotal,
        Integer aforoDisponible
) {
    public static TipoEntradaResponse from(TipoEntrada entrada) {
        return new TipoEntradaResponse(
                entrada.getId(),
                entrada.getNombre(),
                entrada.getPrecio(),
                entrada.getAforoTotal(),
                entrada.getAforoDisponible()
        );
    }
}
