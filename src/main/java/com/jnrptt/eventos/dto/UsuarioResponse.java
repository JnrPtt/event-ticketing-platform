package com.jnrptt.eventos.dto;

import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.model.Usuario;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        Rol rol
) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol()
        );
    }
}
