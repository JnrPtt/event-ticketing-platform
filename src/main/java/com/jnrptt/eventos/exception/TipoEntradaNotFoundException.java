package com.jnrptt.eventos.exception;

public class TipoEntradaNotFoundException extends RuntimeException {
    public TipoEntradaNotFoundException(Long id) {
        super("Tipo de entrada no encontrado con el ID: " + id);
    }
}
