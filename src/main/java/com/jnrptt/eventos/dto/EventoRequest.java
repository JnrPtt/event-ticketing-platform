package com.jnrptt.eventos.dto;

import com.jnrptt.eventos.model.Evento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record EventoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        String descripcion,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,

        @NotBlank(message = "La ciudad es obligatoria")
        String ciudad,

        @NotBlank(message = "El lugar es obligatorio")
        String lugar,

        @NotEmpty(message = "El evento debe tener al menos un tipo de entrada")
        List<@Valid TipoEntradaRequest> tipoEntrada
) {
        public Evento toEvento() {
                Evento evento = new Evento(
                        null,
                        nombre,
                        descripcion,
                        fecha,
                        ciudad,
                        lugar,
                        new ArrayList<>(tipoEntrada.stream()
                                .map(TipoEntradaRequest::toTipoEntrada)
                                .toList())
                );

                evento.getTipoEntrada().forEach(entrada -> entrada.setEvento(evento));
                return evento;
        }
}
