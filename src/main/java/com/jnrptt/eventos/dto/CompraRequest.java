package com.jnrptt.eventos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CompraRequest(
        @NotNull(message = "El usuario es obligatorio")
        Long usuarioId,

        @NotNull(message = "El tipo de entrada es obligatorio")
        Long tipoEntradaId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que 0")
        Integer cantidad
) {
}
