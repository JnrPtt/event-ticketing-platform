package com.jnrptt.eventos.exception;

import com.jnrptt.eventos.model.TiposEntrada;

public class TipoEntradaConVentasException extends RuntimeException {
    public TipoEntradaConVentasException(TiposEntrada tipo, int vendidas) {
        super("No se puede eliminar " + tipo + " porque tiene entradas vendidas (" + vendidas + ")");
    }
}
