package com.jnrptt.eventos.dto;

import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.validation.PasswordValido;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "El password es obligatorio")
        @PasswordValido
        String password
) {
    public RegistroRequest {
        email = Usuario.normalizarEmail(email);
    }

    public Usuario toUsuario(String passwordHash) {
        return new Usuario(null, nombre, email, passwordHash, Rol.USUARIO);
    }
}
