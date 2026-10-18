package com.jnrptt.eventos.exception;

import com.jnrptt.eventos.model.EstadoCompra;

public class TransicionEstadoInvalidaException extends RuntimeException {
    public TransicionEstadoInvalidaException(String accion, EstadoCompra estadoActual) {
        super("No se puede " + accion + " una compra en estado " + estadoActual);
    }
}
