package com.jnrptt.eventos.exception;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Email o password inválidos");
    }
}
