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
}
