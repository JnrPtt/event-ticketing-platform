package com.jnrptt.eventos.exception;

public class CompraNotFoundException extends RuntimeException {
    public CompraNotFoundException(Long id) {
        super("Compra no encontrada con el ID: " + id);
    }
}
