package com.jnrptt.eventos.controller;

import com.jayway.jsonpath.JsonPath;
import com.jnrptt.eventos.TestContainersConfiguration;
import com.jnrptt.eventos.model.Compra;
import com.jnrptt.eventos.model.EstadoCompra;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestContainersConfiguration.class)
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
    void compraCorrectaDevuelve201() throws Exception {
        var escenario = creaEscenario();

        comprar(escenario.usuario().getId(), escenario.tipoEntrada().getId(), 3)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.cantidad").value(3))
                .andExpect(jsonPath("$.total").value(75.0));
    }

    @Test
    void sinAutenticarDevuelve401YNoGuardaNada() throws Exception {
        var escenario = creaEscenario();

        comprarSinToken(escenario.tipoEntrada().getId(), 3)
                .andExpect(status().isUnauthorized());

        assertThat(compraRepository.count()).isZero();
    }

    @Test
    void cantidadCeroDevuelve400() throws Exception {
        var escenario = creaEscenario();

        comprar(escenario.usuario().getId(), escenario.tipoEntrada().getId(), 0)
                .andExpect(status().isBadRequest());

        assertThat(compraRepository.count()).isZero();
    }

    @Test
    void usuarioInexistenteDevuelve404() throws Exception {
        var escenario = creaEscenario();

        comprar(999999L, escenario.tipoEntrada().getId(), 3)
                .andExpect(status().isNotFound());

        assertThat(compraRepository.count()).isZero();
    }

    @Test
    void aforoInsuficienteDevuelve409() throws Exception {
        var escenario = creaEscenario();

        comprar(escenario.usuario().getId(), escenario.tipoEntrada().getId(), AFORO + 1)
                .andExpect(status().isConflict());

        assertThat(compraRepository.count()).isZero();
    }

    @Test
    void pagarDevuelve200ConLaCompraConfirmada() throws Exception {
        var escenario = creaEscenario();
        Long usuarioId = escenario.usuario().getId();
        Long compraId = reservar(escenario, 2);

        accion(usuarioId, compraId, "pagar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(compraId))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"));
    }

    @Test
    void cancelarDevuelve200ConLaCompraCancelada() throws Exception {
        var escenario = creaEscenario();
        Long usuarioId = escenario.usuario().getId();
        Long compraId = reservar(escenario, 2);

        accion(usuarioId, compraId, "cancelar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        assertThat(tipoEntradaRepository.findById(escenario.tipoEntrada().getId()).orElseThrow()
                .getAforoDisponible()).isEqualTo(AFORO);
    }

    @Test
    void pagarDosVecesDevuelve409() throws Exception {
        var escenario = creaEscenario();
        Long usuarioId = escenario.usuario().getId();
        Long compraId = reservar(escenario, 2);
        accion(usuarioId, compraId, "pagar").andExpect(status().isOk());

        accion(usuarioId, compraId, "pagar")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("No se puede pagar una compra en estado CONFIRMADA"));
    }

    @Test
    void pagarOCancelarLaCompraDeOtroUsuarioDevuelve404() throws Exception {
        var escenario = creaEscenario();
        Long compraId = reservar(escenario, 2);
        Long otroUsuarioId = usuarioRepository.save(
                new Usuario(null, "Bea", "bea@example.com", "hash", Rol.USUARIO)).getId();

        accion(otroUsuarioId, compraId, "pagar").andExpect(status().isNotFound());
        accion(otroUsuarioId, compraId, "cancelar").andExpect(status().isNotFound());

        assertThat(compraRepository.findById(compraId).orElseThrow().getEstado()).isEqualTo(EstadoCompra.PENDIENTE);
    }

    @Test
    void pagarSinTokenDevuelve401() throws Exception {
        var escenario = creaEscenario();
        Long compraId = reservar(escenario, 2);

        mockMvc.perform(post("/compras/" + compraId + "/pagar"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listarDevuelveSoloMisComprasPaginadasDeLaMasRecienteALaMasAntigua() throws Exception {
        var escenario = creaEscenario();
        var ahora = LocalDateTime.now();
        Long antigua = guardaCompra(escenario.usuario(), escenario.tipoEntrada(), ahora.minusDays(2)).getId();
        Long media = guardaCompra(escenario.usuario(), escenario.tipoEntrada(), ahora.minusDays(1)).getId();
        Long reciente = guardaCompra(escenario.usuario(), escenario.tipoEntrada(), ahora).getId();
        var bea = usuarioRepository.save(new Usuario(null, "Bea", "bea@example.com", "hash", Rol.USUARIO));
        guardaCompra(bea, escenario.tipoEntrada(), ahora.plusDays(1));

        listar(escenario.usuario().getId(), "?page=0&size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(reciente))
                .andExpect(jsonPath("$.content[1].id").value(media))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        listar(escenario.usuario().getId(), "?page=1&size=2")
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(antigua));
    }

    @Test
    void listarIgnoraElOrdenPedidoPorElCliente() throws Exception {
        var escenario = creaEscenario();
        var ahora = LocalDateTime.now();
        guardaCompra(escenario.usuario(), escenario.tipoEntrada(), ahora.minusDays(1));
        Long reciente = guardaCompra(escenario.usuario(), escenario.tipoEntrada(), ahora).getId();

        listar(escenario.usuario().getId(), "?sort=fecha,asc")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(reciente));
    }

    @Test
    void listarLimitaElTamanoDePaginaA100() throws Exception {
        var escenario = creaEscenario();

        listar(escenario.usuario().getId(), "?size=500")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(100));
    }

    private Long reservar(Escenario escenario, int cantidad) throws Exception {
        String respuesta = comprar(escenario.usuario().getId(), escenario.tipoEntrada().getId(), cantidad)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private ResultActions accion(Long usuarioId, Long compraId, String accion) throws Exception {
        return mockMvc.perform(post("/compras/" + compraId + "/" + accion)
                .with(jwt().jwt(j -> j.subject(String.valueOf(usuarioId)))));
    }

    private ResultActions listar(Long usuarioId, String query) throws Exception {
        return mockMvc.perform(get("/compras" + query)
                .with(jwt().jwt(j -> j.subject(String.valueOf(usuarioId)))));
    }

    private Compra guardaCompra(Usuario usuario, TipoEntrada tipoEntrada, LocalDateTime fecha) {
        return compraRepository.save(new Compra(null, 1, EstadoCompra.CONFIRMADA, fecha,
                tipoEntrada.getPrecio(), usuario, tipoEntrada));
    }

    // El controlador saca el id del usuario del subject del token. jwt() sin más pondría
    // sub = "user" y Long.valueOf("user") acabaría en un 500, así que se fija explícitamente.
    private ResultActions comprar(Long usuarioId, Long tipoEntradaId, int cantidad) throws Exception {
        return mockMvc.perform(peticionCompra(tipoEntradaId, cantidad)
                .with(jwt().jwt(j -> j.subject(String.valueOf(usuarioId)))));
    }

    private ResultActions comprarSinToken(Long tipoEntradaId, int cantidad) throws Exception {
        return mockMvc.perform(peticionCompra(tipoEntradaId, cantidad));
    }

    private MockHttpServletRequestBuilder peticionCompra(Long tipoEntradaId, int cantidad) {
        String body = """
                {"tipoEntradaId": %d, "cantidad": %d}
                """.formatted(tipoEntradaId, cantidad);
        return post("/compras")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    private Escenario creaEscenario() {
        var usuario = usuarioRepository.save(new Usuario(null, "Ana", "ana@example.com", "hash", Rol.USUARIO));
        var evento = eventoRepository.save(new Evento(null, "Concierto", "Concierto de prueba",
                LocalDate.now().plusDays(30), "Madrid", "WiZink Center", new ArrayList<>()));
        var tipoEntrada = tipoEntradaRepository.save(new TipoEntrada(
                null, TiposEntrada.GENERAL, new BigDecimal("25.00"), AFORO, AFORO, evento));
        return new Escenario(usuario, evento, tipoEntrada);
    }

    private record Escenario(Usuario usuario, Evento evento, TipoEntrada tipoEntrada) {
    }
}
