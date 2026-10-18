package com.jnrptt.eventos.controller;

import com.jnrptt.eventos.TestContainersConfiguration;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Comprueba el formato de los errores (ProblemDetail, RFC 9457), no las reglas de negocio,
// que ya cubren los tests de cada controlador y servicio.
@SpringBootTest
@Import(TestContainersConfiguration.class)
@AutoConfigureMockMvc
class ErroresIntegrationTests {

    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

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

    @BeforeEach
    @AfterEach
    void limpiaDatos() {
        compraRepository.deleteAll();
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void validacionFallidaDevuelveProblemDetailConLosCamposErroneos() throws Exception {
        var usuario = usuarioRepository.save(new Usuario(null, "Ana", "ana@example.com", "hash", Rol.USUARIO));

        comprar(usuario.getId(), """
                {"tipoEntradaId": null, "cantidad": 0}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("La petición tiene campos no válidos"))
                .andExpect(jsonPath("$.instance").value("/compras"))
                .andExpect(jsonPath("$.errores.length()").value(2))
                .andExpect(jsonPath("$.errores[?(@.campo == 'cantidad')].mensaje")
                        .value("La cantidad debe ser mayor que 0"))
                .andExpect(jsonPath("$.errores[?(@.campo == 'tipoEntradaId')].mensaje")
                        .value("El tipo de entrada es obligatorio"));
    }

    @Test
    void recursoInexistenteDevuelveProblemDetail404() throws Exception {
        var usuario = usuarioRepository.save(new Usuario(null, "Ana", "ana@example.com", "hash", Rol.USUARIO));

        comprar(usuario.getId(), """
                {"tipoEntradaId": 999999, "cantidad": 1}
                """)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(containsString("999999")));
    }

    @Test
    void aforoInsuficienteDevuelveProblemDetail409() throws Exception {
        var usuario = usuarioRepository.save(new Usuario(null, "Ana", "ana@example.com", "hash", Rol.USUARIO));
        var tipoEntrada = creaTipoEntrada(10, 10);

        comprar(usuario.getId(), """
                {"tipoEntradaId": %d, "cantidad": 11}
                """.formatted(tipoEntrada.getId()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(containsString("solo quedan 10")));
    }

    // Estos dos casos venían de ResponseStatusException en EventoService: comprueban que ahora
    // pasan por las excepciones propias y salen con el mismo formato que el resto.
    @Test
    void tipoDeEntradaRepetidoDevuelveProblemDetail400() throws Exception {
        mockMvc.perform(post("/eventos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ORGANIZADOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventoJson(500, 500)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("El tipo de entrada GENERAL está repetido"));
    }

    @Test
    void reducirAforoPorDebajoDeLoVendidoDevuelveProblemDetail409() throws Exception {
        var tipoEntrada = creaTipoEntrada(100, 70);

        mockMvc.perform(put("/eventos/" + tipoEntrada.getEvento().getId())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ORGANIZADOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "Concierto", "descripcion": "Concierto de prueba", "fecha": "%s",
                                 "ciudad": "Madrid", "lugar": "WiZink Center",
                                 "tipoEntrada": [{"nombre": "GENERAL", "precio": 25.00, "aforoTotal": 20}]}
                                """.formatted(LocalDate.now().plusDays(30))))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value(containsString("entradas vendidas (30)")));
    }

    @Test
    void jsonMalFormadoDevuelveProblemDetail400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": "))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("no es un JSON válido")));
    }

    @Test
    void credencialesInvalidasDevuelvenProblemDetail401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nadie@example.com", "password": "secreto123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Email o password inválidos"));
    }

    private ResultActions comprar(Long usuarioId, String body) throws Exception {
        return mockMvc.perform(post("/compras")
                .with(jwt().jwt(j -> j.subject(String.valueOf(usuarioId))))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private TipoEntrada creaTipoEntrada(int aforoTotal, int aforoDisponible) {
        var evento = eventoRepository.save(new Evento(null, "Concierto", "Concierto de prueba",
                LocalDate.now().plusDays(30), "Madrid", "WiZink Center", new ArrayList<>()));
        return tipoEntradaRepository.save(new TipoEntrada(
                null, TiposEntrada.GENERAL, new BigDecimal("25.00"), aforoTotal, aforoDisponible, evento));
    }

    private String eventoJson(int aforoGeneral, int aforoGeneralRepetido) {
        return """
                {"nombre": "Concierto", "descripcion": "Concierto de prueba", "fecha": "%s",
                 "ciudad": "Madrid", "lugar": "WiZink Center",
                 "tipoEntrada": [
                   {"nombre": "GENERAL", "precio": 25.00, "aforoTotal": %d},
                   {"nombre": "GENERAL", "precio": 30.00, "aforoTotal": %d}
                 ]}
                """.formatted(LocalDate.now().plusDays(30), aforoGeneral, aforoGeneralRepetido);
    }
}
