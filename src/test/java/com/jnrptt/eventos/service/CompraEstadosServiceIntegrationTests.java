package com.jnrptt.eventos.service;

import com.jnrptt.eventos.TestContainersConfiguration;
import com.jnrptt.eventos.dto.CompraRequest;
import com.jnrptt.eventos.exception.CompraNotFoundException;
import com.jnrptt.eventos.exception.TransicionEstadoInvalidaException;
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
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Transiciones PENDIENTE → CONFIRMADA → CANCELADA y devolución del aforo al cancelar.
@SpringBootTest
@Import(TestContainersConfiguration.class)
class CompraEstadosServiceIntegrationTests {

    private static final int AFORO = 10;
    private static final int CANTIDAD = 3;

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
    void pagarUnaCompraPendienteLaConfirma() {
        var escenario = creaEscenario();
        Long compraId = reservar(escenario);

        var respuesta = compraService.pagar(escenario.usuario().getId(), compraId);

        assertThat(respuesta.estado()).isEqualTo(EstadoCompra.CONFIRMADA);
        assertThat(estado(compraId)).isEqualTo(EstadoCompra.CONFIRMADA);
        assertThat(aforoDisponible(escenario)).isEqualTo(AFORO - CANTIDAD);
    }

    @Test
    void pagarDosVecesLanzaTransicionInvalida() {
        var escenario = creaEscenario();
        Long usuarioId = escenario.usuario().getId();
        Long compraId = reservar(escenario);
        compraService.pagar(usuarioId, compraId);

        assertThatThrownBy(() -> compraService.pagar(usuarioId, compraId))
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessage("No se puede pagar una compra en estado CONFIRMADA");

        assertThat(estado(compraId)).isEqualTo(EstadoCompra.CONFIRMADA);
    }

    @Test
    void pagarUnaCompraCanceladaLanzaTransicionInvalida() {
        var escenario = creaEscenario();
        Long usuarioId = escenario.usuario().getId();
        Long compraId = reservar(escenario);
        compraService.cancelar(usuarioId, compraId);

        assertThatThrownBy(() -> compraService.pagar(usuarioId, compraId))
                .isInstanceOf(TransicionEstadoInvalidaException.class);

        assertThat(estado(compraId)).isEqualTo(EstadoCompra.CANCELADA);
        assertThat(aforoDisponible(escenario)).isEqualTo(AFORO);
    }

    @Test
    void cancelarUnaCompraPendienteDevuelveElAforo() {
        var escenario = creaEscenario();
        Long compraId = reservar(escenario);
        assertThat(aforoDisponible(escenario)).isEqualTo(AFORO - CANTIDAD);

        var respuesta = compraService.cancelar(escenario.usuario().getId(), compraId);

        assertThat(respuesta.estado()).isEqualTo(EstadoCompra.CANCELADA);
        assertThat(estado(compraId)).isEqualTo(EstadoCompra.CANCELADA);
        assertThat(aforoDisponible(escenario)).isEqualTo(AFORO);
    }

    @Test
    void cancelarUnaCompraConfirmadaDevuelveElAforo() {
        var escenario = creaEscenario();
        Long usuarioId = escenario.usuario().getId();
        Long compraId = reservar(escenario);
        compraService.pagar(usuarioId, compraId);

        compraService.cancelar(usuarioId, compraId);

        assertThat(estado(compraId)).isEqualTo(EstadoCompra.CANCELADA);
        assertThat(aforoDisponible(escenario)).isEqualTo(AFORO);
    }

    @Test
    void cancelarDosVecesLanzaTransicionInvalidaYNoDevuelveElAforoOtraVez() {
        var escenario = creaEscenario();
        Long usuarioId = escenario.usuario().getId();
        Long compraId = reservar(escenario);
        compraService.cancelar(usuarioId, compraId);

        assertThatThrownBy(() -> compraService.cancelar(usuarioId, compraId))
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessage("No se puede cancelar una compra en estado CANCELADA");

        assertThat(aforoDisponible(escenario)).isEqualTo(AFORO);
    }

    @Test
    void laCompraDeOtroUsuarioNoSePuedePagarNiCancelar() {
        var escenario = creaEscenario();
        Long compraId = reservar(escenario);
        Long otroUsuarioId = usuarioRepository.save(
                new Usuario(null, "Bea", "bea@example.com", "hash", Rol.USUARIO)).getId();

        assertThatThrownBy(() -> compraService.pagar(otroUsuarioId, compraId))
                .isInstanceOf(CompraNotFoundException.class);
        assertThatThrownBy(() -> compraService.cancelar(otroUsuarioId, compraId))
                .isInstanceOf(CompraNotFoundException.class);

        assertThat(estado(compraId)).isEqualTo(EstadoCompra.PENDIENTE);
        assertThat(aforoDisponible(escenario)).isEqualTo(AFORO - CANTIDAD);
    }

    // Sin el bloqueo de la compra, varias cancelaciones simultáneas leerían todas CONFIRMADA
    // y cada una devolvería el aforo: el tipo de entrada acabaría con más aforo del que tiene.
    @Test
    void cancelacionesConcurrentesSoloDevuelvenElAforoUnaVez() throws Exception {
        var escenario = creaEscenario();
        Long usuarioId = escenario.usuario().getId();
        Long compraId = reservar(escenario);
        compraService.pagar(usuarioId, compraId);
        int hilos = 10;

        ExecutorService executor = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Boolean>> resultados = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            resultados.add(executor.submit(() -> {
                salida.await();
                try {
                    compraService.cancelar(usuarioId, compraId);
                    return true;
                } catch (TransicionEstadoInvalidaException e) {
                    return false;
                }
            }));
        }

        salida.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS))
                .as("Las cancelaciones concurrentes no terminaron a tiempo")
                .isTrue();

        int exitos = 0;
        for (Future<Boolean> resultado : resultados) {
            try {
                if (resultado.get()) {
                    exitos++;
                }
            } catch (ExecutionException e) {
                throw new AssertionError("Una cancelación lanzó una excepción inesperada", e.getCause());
            }
        }

        assertThat(exitos).isEqualTo(1);
        assertThat(estado(compraId)).isEqualTo(EstadoCompra.CANCELADA);
        assertThat(aforoDisponible(escenario)).isEqualTo(AFORO);
    }

    private Long reservar(Escenario escenario) {
        return compraService.comprar(escenario.usuario().getId(),
                new CompraRequest(escenario.tipoEntrada().getId(), CANTIDAD)).id();
    }

    private EstadoCompra estado(Long compraId) {
        return compraRepository.findById(compraId).orElseThrow().getEstado();
    }

    private int aforoDisponible(Escenario escenario) {
        return tipoEntradaRepository.findById(escenario.tipoEntrada().getId())
                .orElseThrow()
                .getAforoDisponible();
    }

    private Escenario creaEscenario() {
        var usuario = usuarioRepository.save(new Usuario(null, "Ana", "ana@example.com", "hash", Rol.USUARIO));
        var evento = eventoRepository.save(new Evento(null, "Concierto", "Concierto de prueba",
                LocalDate.now().plusDays(30), "Madrid", "WiZink Center", new ArrayList<>()));
        var tipoEntrada = tipoEntradaRepository.save(new TipoEntrada(
                null, TiposEntrada.GENERAL, new BigDecimal("25.00"), AFORO, AFORO, evento));
        return new Escenario(usuario, tipoEntrada);
    }

    private record Escenario(Usuario usuario, TipoEntrada tipoEntrada) {
    }
}
