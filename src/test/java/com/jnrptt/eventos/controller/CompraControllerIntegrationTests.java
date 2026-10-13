package com.jnrptt.eventos.controller;

import com.jnrptt.eventos.model.Evento;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CompraControllerIntegrationTests {

    private static final int AFORO = 10;

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
    @WithMockUser
    void compraCorrectaDevuelve201() throws Exception {
        var escenario = creaEscenario();

        comprar(escenario.usuario().getId(), escenario.tipoEntrada().getId(), 3)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.cantidad").value(3))
                .andExpect(jsonPath("$.total").value(75.0));
    }

    @Test
    void sinAutenticarDevuelve401YNoGuardaNada() throws Exception {
        var escenario = creaEscenario();

        comprar(escenario.usuario().getId(), escenario.tipoEntrada().getId(), 3)
                .andExpect(status().isUnauthorized());

        assertThat(compraRepository.count()).isZero();
    }

    @Test
    @WithMockUser
    void cantidadCeroDevuelve400() throws Exception {
        var escenario = creaEscenario();

        comprar(escenario.usuario().getId(), escenario.tipoEntrada().getId(), 0)
                .andExpect(status().isBadRequest());

        assertThat(compraRepository.count()).isZero();
    }

    @Test
    @WithMockUser
    void usuarioInexistenteDevuelve404() throws Exception {
        var escenario = creaEscenario();

        comprar(999999L, escenario.tipoEntrada().getId(), 3)
                .andExpect(status().isNotFound());

        assertThat(compraRepository.count()).isZero();
    }

    @Test
    @WithMockUser
    void aforoInsuficienteDevuelve409() throws Exception {
        var escenario = creaEscenario();

        comprar(escenario.usuario().getId(), escenario.tipoEntrada().getId(), AFORO + 1)
                .andExpect(status().isConflict());

        assertThat(compraRepository.count()).isZero();
    }

    private ResultActions comprar(Long usuarioId, Long tipoEntradaId, int cantidad) throws Exception {
        String body = """
                {"usuarioId": %d, "tipoEntradaId": %d, "cantidad": %d}
                """.formatted(usuarioId, tipoEntradaId, cantidad);
        return mockMvc.perform(post("/compras")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Escenario creaEscenario() {
        var usuario = usuarioRepository.save(new Usuario(null, "Ana", "ana@example.com"));
        var evento = eventoRepository.save(new Evento(null, "Concierto", "Concierto de prueba",
                LocalDate.now().plusDays(30), "Madrid", "WiZink Center", new ArrayList<>()));
        var tipoEntrada = tipoEntradaRepository.save(new TipoEntrada(
                null, TiposEntrada.GENERAL, new BigDecimal("25.00"), AFORO, AFORO, evento));
        return new Escenario(usuario, evento, tipoEntrada);
    }

    private record Escenario(Usuario usuario, Evento evento, TipoEntrada tipoEntrada) {
    }
}
