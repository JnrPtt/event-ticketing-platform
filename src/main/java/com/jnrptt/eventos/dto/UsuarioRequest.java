package com.jnrptt.eventos.dto;

import com.jnrptt.eventos.model.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email
) {
    public Usuario toUsuario() {
        return new Usuario(null, nombre, email);
    }
}
