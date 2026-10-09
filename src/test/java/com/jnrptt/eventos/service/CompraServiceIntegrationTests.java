package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.CompraRequest;
import com.jnrptt.eventos.exception.AforoInsuficienteException;
import com.jnrptt.eventos.exception.UsuarioNotFoundException;
import com.jnrptt.eventos.model.EstadoCompra;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class CompraServiceIntegrationTests {

    private static final int AFORO = 10;

    @Autowired
    private CompraService compraService;

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
    void compraCorrectaDescuentaAforoYGuardaLaCompra() {
        var escenario = creaEscenario();

        var respuesta = compraService.comprar(new CompraRequest(
                escenario.usuario().getId(), escenario.tipoEntrada().getId(), 3));

        assertThat(respuesta.id()).isNotNull();
        assertThat(respuesta.estado()).isEqualTo(EstadoCompra.CONFIRMADA);
        assertThat(respuesta.cantidad()).isEqualTo(3);
        assertThat(respuesta.precioUnitario()).isEqualByComparingTo("25");
        assertThat(respuesta.total()).isEqualByComparingTo("75");
        assertThat(respuesta.usuarioId()).isEqualTo(escenario.usuario().getId());
        assertThat(respuesta.tipoEntradaId()).isEqualTo(escenario.tipoEntrada().getId());
        assertThat(respuesta.eventoId()).isEqualTo(escenario.evento().getId());

        assertThat(aforoDisponible(escenario)).isEqualTo(7);
        assertThat(compraRepository.count()).isEqualTo(1);
    }

    @Test
    void aforoInsuficienteLanzaExcepcionYNoModificaNada() {
        var escenario = creaEscenario();

        assertThatThrownBy(() -> compraService.comprar(new CompraRequest(
                escenario.usuario().getId(), escenario.tipoEntrada().getId(), AFORO + 1)))
                .isInstanceOf(AforoInsuficienteException.class);

        assertThat(aforoDisponible(escenario)).isEqualTo(AFORO);
        assertThat(compraRepository.count()).isZero();
    }

    @Test
    void comprarExactamenteElAforoDisponibleLoDejaACero() {
        var escenario = creaEscenario();

        compraService.comprar(new CompraRequest(
                escenario.usuario().getId(), escenario.tipoEntrada().getId(), AFORO));

        assertThat(aforoDisponible(escenario)).isZero();
        assertThat(compraRepository.count()).isEqualTo(1);
    }

    @Test
    void usuarioInexistenteLanzaExcepcionYNoModificaNada() {
        var escenario = creaEscenario();

        assertThatThrownBy(() -> compraService.comprar(new CompraRequest(
                999999L, escenario.tipoEntrada().getId(), 3)))
                .isInstanceOf(UsuarioNotFoundException.class);

        assertThat(compraRepository.count()).isZero();
        assertThat(aforoDisponible(escenario)).isEqualTo(AFORO);
    }

    private int aforoDisponible(Escenario escenario) {
        return tipoEntradaRepository.findById(escenario.tipoEntrada().getId())
                .orElseThrow()
                .getAforoDisponible();
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
