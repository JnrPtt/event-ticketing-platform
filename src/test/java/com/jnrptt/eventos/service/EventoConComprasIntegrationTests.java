package com.jnrptt.eventos.service;

import com.jnrptt.eventos.TestContainersConfiguration;
import com.jnrptt.eventos.dto.CompraRequest;
import com.jnrptt.eventos.dto.EventoRequest;
import com.jnrptt.eventos.dto.TipoEntradaRequest;
import com.jnrptt.eventos.exception.AforoInsuficienteException;
import com.jnrptt.eventos.exception.EventoConComprasException;
import com.jnrptt.eventos.exception.TipoEntradaConComprasException;
import com.jnrptt.eventos.model.Compra;
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

@SpringBootTest
@Import(TestContainersConfiguration.class)
class EventoConComprasIntegrationTests {

    private static final int AFORO = 100;

    @Autowired
    private EventoService eventoService;

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

    private Usuario usuario;

    @BeforeEach
    void preparaDatos() {
        limpiaDatos();
        usuario = usuarioRepository.save(new Usuario(null, "Ana", "ana@example.com", "hash", Rol.USUARIO));
    }

    @AfterEach
    void limpiaDatos() {
        compraRepository.deleteAll();
        eventoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void borrarUnEventoConComprasLanzaConflictoYNoBorraNada() {
        var evento = crearEvento(entrada(TiposEntrada.GENERAL, AFORO));
        comprar(tipo(evento, TiposEntrada.GENERAL), 2);

        assertThatThrownBy(() -> eventoService.deleteEventoById(evento.getId()))
                .isInstanceOf(EventoConComprasException.class)
                .hasMessage("No se puede eliminar el evento " + evento.getId() + " porque tiene compras");

        assertThat(eventoRepository.existsById(evento.getId())).isTrue();
        assertThat(compraRepository.count()).isEqualTo(1);
    }

    @Test
    void borrarUnEventoConSoloComprasCanceladasTambienLanzaConflicto() {
        var evento = crearEvento(entrada(TiposEntrada.GENERAL, AFORO));
        Long compraId = comprar(tipo(evento, TiposEntrada.GENERAL), 2);
        compraService.cancelar(usuario.getId(), compraId);

        assertThatThrownBy(() -> eventoService.deleteEventoById(evento.getId()))
                .isInstanceOf(EventoConComprasException.class);

        assertThat(eventoRepository.existsById(evento.getId())).isTrue();
    }

    @Test
    void borrarUnEventoSinComprasLoBorraConSusTiposDeEntrada() {
        var evento = crearEvento(entrada(TiposEntrada.GENERAL, AFORO), entrada(TiposEntrada.VIP, 10));

        eventoService.deleteEventoById(evento.getId());

        assertThat(eventoRepository.existsById(evento.getId())).isFalse();
        assertThat(tipoEntradaRepository.count()).isZero();
    }

    @Test
    void quitarUnTipoDeEntradaConComprasCanceladasLanzaConflictoYNoCambiaNada() {
        var evento = crearEvento(entrada(TiposEntrada.GENERAL, AFORO), entrada(TiposEntrada.VIP, 10));
        Long compraId = comprar(tipo(evento, TiposEntrada.VIP), 1);
        compraService.cancelar(usuario.getId(), compraId);

        assertThatThrownBy(() -> eventoService.editEventoById(evento.getId(),
                peticion("Concierto editado", entrada(TiposEntrada.GENERAL, AFORO))))
                .isInstanceOf(TipoEntradaConComprasException.class)
                .hasMessage("No se puede eliminar VIP porque tiene compras");

        assertThat(tipoEntradaRepository.count()).isEqualTo(2);
        assertThat(eventoService.getEventoById(evento.getId()).getNombre()).isEqualTo("Concierto");
    }

    @Test
    void quitarUnTipoDeEntradaSinComprasLoElimina() {
        var evento = crearEvento(entrada(TiposEntrada.GENERAL, AFORO), entrada(TiposEntrada.VIP, 10));
        comprar(tipo(evento, TiposEntrada.GENERAL), 1);

        eventoService.editEventoById(evento.getId(), peticion("Concierto", entrada(TiposEntrada.GENERAL, AFORO)));

        assertThat(tipoEntradaRepository.count()).isEqualTo(1);
    }

    @Test
    void editarElEventoMientrasSeCompraNoPierdeNingunaVenta() throws Exception {
        var evento = crearEvento(entrada(TiposEntrada.GENERAL, AFORO));
        Long tipoEntradaId = tipo(evento, TiposEntrada.GENERAL).getId();
        int compradores = 40;
        int ediciones = 20;

        ExecutorService executor = Executors.newFixedThreadPool(compradores + 1);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<?>> tareas = new ArrayList<>();
        for (int i = 0; i < compradores; i++) {
            tareas.add(executor.submit(() -> {
                salida.await();
                try {
                    compraService.comprar(usuario.getId(), new CompraRequest(tipoEntradaId, 1));
                } catch (AforoInsuficienteException e) {
                    throw new AssertionError("No debería faltar aforo", e);
                }
                return null;
            }));
        }
        tareas.add(executor.submit(() -> {
            salida.await();
            for (int i = 0; i < ediciones; i++) {
                eventoService.editEventoById(evento.getId(), peticion("Concierto " + i,
                        new TipoEntradaRequest(TiposEntrada.GENERAL, new BigDecimal(30 + i), AFORO)));
            }
            return null;
        }));

        salida.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(60, TimeUnit.SECONDS))
                .as("Las compras y ediciones concurrentes no terminaron a tiempo")
                .isTrue();
        for (Future<?> tarea : tareas) {
            try {
                tarea.get();
            } catch (ExecutionException e) {
                throw new AssertionError("Una compra o edición lanzó una excepción inesperada", e.getCause());
            }
        }

        int vendidas = compraRepository.findAll().stream().mapToInt(Compra::getCantidad).sum();
        int disponible = tipoEntradaRepository.findById(tipoEntradaId).orElseThrow().getAforoDisponible();
        assertThat(vendidas).isEqualTo(compradores);
        assertThat(disponible + vendidas).isEqualTo(AFORO);
    }

    private Evento crearEvento(TipoEntradaRequest... entradas) {
        return eventoService.createEvento(peticion("Concierto", entradas));
    }

    private TipoEntrada tipo(Evento evento, TiposEntrada nombre) {
        return evento.getTipoEntrada().stream()
                .filter(tipo -> tipo.getNombre() == nombre)
                .findFirst()
                .orElseThrow();
    }

    private Long comprar(TipoEntrada tipoEntrada, int cantidad) {
        return compraService.comprar(usuario.getId(), new CompraRequest(tipoEntrada.getId(), cantidad)).id();
    }

    private static EventoRequest peticion(String nombre, TipoEntradaRequest... entradas) {
        return new EventoRequest(nombre, "Concierto de prueba", LocalDate.now().plusDays(30),
                "Madrid", "WiZink Center", List.of(entradas));
    }

    private static TipoEntradaRequest entrada(TiposEntrada nombre, int aforoTotal) {
        return new TipoEntradaRequest(nombre, new BigDecimal("25.00"), aforoTotal);
    }
}
