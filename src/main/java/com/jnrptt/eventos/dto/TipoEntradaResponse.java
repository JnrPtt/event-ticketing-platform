package com.jnrptt.eventos.dto;

import com.jnrptt.eventos.model.TiposEntrada;

import java.math.BigDecimal;

public record TipoEntradaResponse(
        Long id,
        TiposEntrada nombre,
        BigDecimal precio,
        Integer aforoTotal,
        Integer aforoDisponible
) {
}
