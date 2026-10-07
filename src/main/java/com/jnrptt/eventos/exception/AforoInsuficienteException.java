package com.jnrptt.eventos.exception;

public class AforoInsuficienteException extends RuntimeException {
    public AforoInsuficienteException(int solicitadas, int disponibles) {
        super("Aforo insuficiente: se solicitaron " + solicitadas + " entradas y solo quedan " + disponibles);
    }
}
