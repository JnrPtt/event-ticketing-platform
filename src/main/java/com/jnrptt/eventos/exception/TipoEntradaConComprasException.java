package com.jnrptt.eventos.exception;

import com.jnrptt.eventos.model.TiposEntrada;

public class TipoEntradaConComprasException extends RuntimeException {
    public TipoEntradaConComprasException(TiposEntrada tipo) {
        super("No se puede eliminar " + tipo + " porque tiene compras");
    }
}
