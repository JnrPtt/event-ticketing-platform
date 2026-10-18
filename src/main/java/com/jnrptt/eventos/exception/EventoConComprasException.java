package com.jnrptt.eventos.exception;

public class EventoConComprasException extends RuntimeException {
    public EventoConComprasException(Long id) {
        super("No se puede eliminar el evento " + id + " porque tiene compras");
    }
}
