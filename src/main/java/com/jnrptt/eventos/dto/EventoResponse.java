package com.jnrptt.eventos.dto;

import java.util.List;

public record EventoResponse(
        Long id,
        String nombre,
        String descripcion,
        String fecha,
        String ciudad,
        String lugar,
        List<TipoEntradaResponse> tiposEntrada
) {
}
