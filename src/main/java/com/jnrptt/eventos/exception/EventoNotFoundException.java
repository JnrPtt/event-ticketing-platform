package com.jnrptt.eventos.exception;

public class EventoNotFoundException extends RuntimeException {
    public EventoNotFoundException(Long id) {
        super("No se encontro el evento con id: " + id);
    }
}
