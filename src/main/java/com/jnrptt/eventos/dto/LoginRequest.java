package com.jnrptt.eventos.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El email es obligatorio")
        String email,

        @NotBlank(message = "El password es obligatorio")
        String password
) {
}
