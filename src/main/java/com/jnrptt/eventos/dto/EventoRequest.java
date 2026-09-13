package com.jnrptt.eventos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EventoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        String descripcion,

        @NotBlank(message = "La fecha es obligatoria")
        String fecha,

        @NotBlank(message = "La ciudad es obligatoria")
        String ciudad,

        @NotBlank(message = "El lugar es obligatorio")
        String lugar,

        @NotEmpty(message = "El evento debe tener al menos un tipo de entrada")
        List<@Valid TipoEntradaRequest> tiposEntrada
) {
}
