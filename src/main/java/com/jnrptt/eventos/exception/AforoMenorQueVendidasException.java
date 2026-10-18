package com.jnrptt.eventos.exception;

import com.jnrptt.eventos.model.TiposEntrada;

public class AforoMenorQueVendidasException extends RuntimeException {
    public AforoMenorQueVendidasException(TiposEntrada tipo, int vendidas) {
        super("El aforo de " + tipo + " no puede ser menor que las entradas vendidas (" + vendidas + ")");
    }
}
