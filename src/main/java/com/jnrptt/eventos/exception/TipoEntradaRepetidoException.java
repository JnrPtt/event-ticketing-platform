package com.jnrptt.eventos.exception;

import com.jnrptt.eventos.model.TiposEntrada;

public class TipoEntradaRepetidoException extends RuntimeException {
    public TipoEntradaRepetidoException(TiposEntrada tipo) {
        super("El tipo de entrada " + tipo + " está repetido");
    }
}
