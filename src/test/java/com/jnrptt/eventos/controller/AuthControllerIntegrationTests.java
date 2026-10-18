package com.jnrptt.eventos.controller;

import com.jnrptt.eventos.TestContainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.repository.UsuarioRepository;
import com.jnrptt.eventos.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestContainersConfiguration.class)
@AutoConfigureMockMvc
class AuthControllerIntegrationTests {

    private static final String PASSWORD = "secreto123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private UsuarioService usuarioService;

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

    @Test
    void passwordDe72CaracteresPeroMasDe72BytesDevuelve400() throws Exception {
        registrar("ana@example.com", "ñ".repeat(72))
                .andExpect(status().isBadRequest());

        assertThat(usuarioRepository.count()).isZero();
    }

    @Test
    void loginCorrectoDevuelve200YUnTokenConElIdDelUsuarioComoSub() throws Exception {
        registrar("ana@example.com", PASSWORD).andExpect(status().isCreated());
        Long id = usuarioRepository.findByEmail("ana@example.com").orElseThrow().getId();

        String respuesta = login("ana@example.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(3600))
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(respuesta, "$.token");
        assertThat(token).isNotBlank();
        assertThat(jwtDecoder.decode(token).getSubject()).isEqualTo(String.valueOf(id));
    }

    @Test
    void loginConElEmailEnMayusculasDevuelve200() throws Exception {
        registrar("ana@example.com", PASSWORD).andExpect(status().isCreated());

        login("ANA@EXAMPLE.COM", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginConPasswordIncorrectoDevuelve401() throws Exception {
        registrar("ana@example.com", PASSWORD).andExpect(status().isCreated());

        login("ana@example.com", "otroPassword")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConEmailInexistenteDevuelve401ConElMismoBodyQuePasswordIncorrecto() throws Exception {
        registrar("ana@example.com", PASSWORD).andExpect(status().isCreated());

        String bodyPasswordIncorrecto = login("ana@example.com", "otroPassword")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        login("nadie@example.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(bodyPasswordIncorrecto));
    }

    @Test
    void loginDelOrganizadorInicialDevuelveUnTokenConRolOrganizador() throws Exception {
        usuarioService.crearOrganizadorSiNoExiste("Org", "org@example.com", PASSWORD);

        String respuesta = login("org@example.com", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(respuesta, "$.token");
        assertThat(jwtDecoder.decode(token).getClaimAsStringList("roles")).containsExactly("ORGANIZADOR");
    }

    private ResultActions login(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
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
