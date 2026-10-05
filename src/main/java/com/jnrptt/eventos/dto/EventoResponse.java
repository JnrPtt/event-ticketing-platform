package com.jnrptt.eventos.dto;

import com.jnrptt.eventos.model.Evento;

import java.time.LocalDate;
import java.util.List;

public record EventoResponse(
        Long id,
        String nombre,
        String descripcion,
        LocalDate fecha,
        String ciudad,
        String lugar,
        List<TipoEntradaResponse> tiposEntrada
) {
    public static EventoResponse from(Evento evento) {
        return new EventoResponse(
                evento.getId(),
                evento.getNombre(),
                evento.getDescripcion(),
                evento.getFecha(),
                evento.getCiudad(),
                evento.getLugar(),
                evento.getTipoEntrada().stream().map(TipoEntradaResponse::from).toList()
        );
    }
}
