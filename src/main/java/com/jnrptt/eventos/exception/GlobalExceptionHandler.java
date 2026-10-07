package com.jnrptt.eventos.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EventoNotFoundException.class)
    public ResponseEntity<String> handleEventoNotFoundException(EventoNotFoundException exception) {
        return ResponseEntity.status(404).body(exception.getMessage());
    }

    @ExceptionHandler(UsuarioNotFoundException.class)
    public ResponseEntity<String> handleUsuarioNotFoundException(UsuarioNotFoundException exception) {
        return ResponseEntity.status(404).body(exception.getMessage());
    }

    @ExceptionHandler(TipoEntradaNotFoundException.class)
    public ResponseEntity<String> handleTipoEntradaNotFoundException(TipoEntradaNotFoundException exception) {
        return ResponseEntity.status(404).body(exception.getMessage());
    }

    @ExceptionHandler(AforoInsuficienteException.class)
    public ResponseEntity<String> handleAforoInsuficienteException(AforoInsuficienteException exception) {
        return ResponseEntity.status(409).body(exception.getMessage());
    }

}
