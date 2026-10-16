package com.jnrptt.eventos.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

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

    @ExceptionHandler(EmailYaRegistradoException.class)
    public ResponseEntity<String> handleEmailYaRegistradoException(EmailYaRegistradoException exception) {
        return ResponseEntity.status(409).body(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(400).body(message);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<String> handleCredencialesInvalidasException(CredencialesInvalidasException exception) {
        return ResponseEntity.status(401).body(exception.getMessage());
    }
}
