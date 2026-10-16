package com.jnrptt.eventos.controller;

import com.jayway.jsonpath.JsonPath;
import com.jnrptt.eventos.model.Evento;
import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.model.TipoEntrada;
import com.jnrptt.eventos.model.TiposEntrada;
import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.repository.CompraRepository;
import com.jnrptt.eventos.repository.EventoRepository;
import com.jnrptt.eventos.repository.TipoEntradaRepository;
import com.jnrptt.eventos.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SeguridadIntegrationTests {

    private static final String PASSWORD = "secreto123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private TipoEntradaRepository tipoEntradaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @AfterEach
    void limpiaDatos() {
        compraRepository.deleteAll();
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void listarEventosSinTokenDevuelve200() throws Exception {
        mockMvc.perform(get("/eventos"))
                .andExpect(status().isOk());
    }

    @Test
    void crearEventoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(peticionCrearEvento())
                .andExpect(status().isUnauthorized());

        assertThat(eventoRepository.count()).isZero();
    }

    @Test
    void crearEventoConRolUsuarioDevuelve403() throws Exception {
        mockMvc.perform(peticionCrearEvento()
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USUARIO"))))
                .andExpect(status().isForbidden());

        assertThat(eventoRepository.count()).isZero();
    }

    @Test
    void crearEventoConRolOrganizadorDevuelve201() throws Exception {
        mockMvc.perform(peticionCrearEvento()
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ORGANIZADOR"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Concierto"));

        assertThat(eventoRepository.count()).isEqualTo(1);
    }

    @Test
    void registroLoginYCompraConTokenRealDevuelve201() throws Exception {
        var tipoEntrada = creaTipoEntrada();
        Long usuarioId = registrar("ana@example.com");
        String token = login("ana@example.com");

        comprar(usuarioId, tipoEntrada.getId(), "Bearer " + token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        assertThat(compraRepository.count()).isEqualTo(1);
    }

    @Test
    void tokenRealDeOrganizadorAlCrearEventoDevuelve201() throws Exception {
        // Único caso en que el rol leído del token abre una puerta: si el converter no
        // traduce el claim "roles" a ROLE_ORGANIZADOR, esta petición recibe 403.
        usuarioRepository.save(new Usuario(null, "Org", "org@example.com",
                passwordEncoder.encode(PASSWORD), Rol.ORGANIZADOR));
        String token = login("org@example.com");

        mockMvc.perform(peticionCrearEvento()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isCreated());

        assertThat(eventoRepository.count()).isEqualTo(1);
    }

    @Test
    void tokenRealDeUsuarioAlCrearEventoDevuelve403() throws Exception {
        registrar("ana@example.com");
        String token = login("ana@example.com");

        mockMvc.perform(peticionCrearEvento()
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(eventoRepository.count()).isZero();
    }

    @Test
    void tokenManipuladoDevuelve401() throws Exception {
        var tipoEntrada = creaTipoEntrada();
        Long usuarioId = registrar("ana@example.com");
        String token = login("ana@example.com");

        comprar(usuarioId, tipoEntrada.getId(), "Bearer " + manipula(token))
                .andExpect(status().isUnauthorized());

        assertThat(compraRepository.count()).isZero();
    }

    // Cambia el primer carácter de la firma. No se toca el último porque en base64url
    // sus bits finales pueden ser de relleno y el cambio no alteraría la firma decodificada.
    private String manipula(String token) {
        int inicioFirma = token.lastIndexOf('.') + 1;
        char original = token.charAt(inicioFirma);
        char cambiado = original == 'A' ? 'B' : 'A';
        return token.substring(0, inicioFirma) + cambiado + token.substring(inicioFirma + 1);
    }

    private ResultActions comprar(Long usuarioId, Long tipoEntradaId, String authorization) throws Exception {
        String body = """
                {"usuarioId": %d, "tipoEntradaId": %d, "cantidad": 2}
                """.formatted(usuarioId, tipoEntradaId);
        return mockMvc.perform(post("/compras")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Long registrar(String email) throws Exception {
        String body = """
                {"nombre": "Ana", "email": "%s", "password": "%s"}
                """.formatted(email, PASSWORD);
        String respuesta = mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private String login(String email) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, PASSWORD);
        String respuesta = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private MockHttpServletRequestBuilder peticionCrearEvento() {
        String body = """
                {
                  "nombre": "Concierto",
                  "descripcion": "Concierto de prueba",
                  "fecha": "%s",
                  "ciudad": "Madrid",
                  "lugar": "WiZink Center",
                  "tipoEntrada": [
                    {"nombre": "GENERAL", "precio": 25.00, "aforoTotal": 100}
                  ]
                }
                """.formatted(LocalDate.now().plusDays(30));
        return post("/eventos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private TipoEntrada creaTipoEntrada() {
        var evento = eventoRepository.save(new Evento(null, "Concierto", "Concierto de prueba",
                LocalDate.now().plusDays(30), "Madrid", "WiZink Center", new ArrayList<>()));
        return tipoEntradaRepository.save(new TipoEntrada(
                null, TiposEntrada.GENERAL, new BigDecimal("25.00"), 10, 10, evento));
    }
}
