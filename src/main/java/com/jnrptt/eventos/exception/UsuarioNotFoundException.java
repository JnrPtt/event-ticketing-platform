package com.jnrptt.eventos.exception;

public class UsuarioNotFoundException extends RuntimeException {
    public UsuarioNotFoundException(Long id) {
        super("Usuario no encontrado con el ID: " + id);
    }
}
