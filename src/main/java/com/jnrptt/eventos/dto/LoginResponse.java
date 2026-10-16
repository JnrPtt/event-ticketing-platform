package com.jnrptt.eventos.dto;

import java.time.Duration;

public record LoginResponse(String token, String tipo, long expiraEnSegundos) {
    public static LoginResponse from(String token, Duration expiracion) {
        return new LoginResponse(token, "Bearer", expiracion.toSeconds());
    }
}
