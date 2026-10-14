package com.jnrptt.eventos.service;

import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TokenServiceIntegrationTests {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void tokenGeneradoSeDecodificaConSubYRoles() {
        Usuario usuario = new Usuario(1L, "Ana", "ana@example.com", "{noop}secreta", Rol.ORGANIZADOR);

        Jwt jwt = jwtDecoder.decode(tokenService.generar(usuario));

        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ORGANIZADOR");
    }
}
