package com.jnrptt.eventos.validation;

import java.nio.charset.StandardCharsets;

public final class PoliticaPassword {
    public static final int MIN_CARACTERES = 8;
    public static final int MAX_BYTES = 72;
    public static final String MENSAJE = "El password debe tener al menos " + MIN_CARACTERES
            + " caracteres y como máximo " + MAX_BYTES + " bytes";

    private PoliticaPassword() {
    }

    public static boolean cumple(String password) {
        return password != null
                && password.length() >= MIN_CARACTERES
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }
}
