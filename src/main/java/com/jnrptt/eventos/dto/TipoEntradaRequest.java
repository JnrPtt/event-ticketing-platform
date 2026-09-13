package com.jnrptt.eventos.dto;

import com.jnrptt.eventos.model.TiposEntrada;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TipoEntradaRequest(
        @NotNull(message = "El tipo de entrada es obligatorio")
        TiposEntrada nombre,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que 0")
        BigDecimal precio,

        @NotNull(message = "El aforo total es obligatorio")
        @Positive(message = "El aforo total debe ser mayor que 0")
        Integer aforoTotal
) {
}
