package com.jnrptt.eventos.controller;

import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTests {

    private static final String PASSWORD = "secreto123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void limpiaUsuarios() {
        usuarioRepository.deleteAll();
    }

    @Test
    void registroCorrectoDevuelve201YGuardaElPasswordEnHash() throws Exception {
        registrar("ana@example.com", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.rol").value("USUARIO"))
                .andExpect(jsonPath("$.password").doesNotExist());

        var guardado = usuarioRepository.findByEmail("ana@example.com").orElseThrow();
        assertThat(guardado.getRol()).isEqualTo(Rol.USUARIO);
        assertThat(guardado.getPassword()).isNotEqualTo(PASSWORD);
        assertThat(guardado.getPassword()).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches(PASSWORD, guardado.getPassword())).isTrue();
    }

    @Test
    void emailRepetidoDevuelve409() throws Exception {
        registrar("ana@example.com", PASSWORD).andExpect(status().isCreated());

        registrar("ana@example.com", PASSWORD)
                .andExpect(status().isConflict());

        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void emailRepetidoConOtrasMayusculasYEspaciosDevuelve409() throws Exception {
        registrar("Ana@Example.COM", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ana@example.com"));

        registrar("  ana@example.com ", PASSWORD)
                .andExpect(status().isConflict());

        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    void passwordCortoDevuelve400() throws Exception {
        registrar("ana@example.com", "corto")
                .andExpect(status().isBadRequest());

        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void passwordDeMasDe72BytesDevuelve400() throws Exception {
        // BCrypt solo admite hasta 72 bytes; por encima lanza IllegalArgumentException.
        registrar("ana@example.com", "a".repeat(73))
                .andExpect(status().isBadRequest());

        assertThat(usuarioRepository.count()).isZero();
    }

    private ResultActions registrar(String email, String password) throws Exception {
        String body = """
                {"nombre": "Ana", "email": "%s", "password": "%s"}
                """.formatted(email, password);
        return mockMvc.perform(post("/auth/registro")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
