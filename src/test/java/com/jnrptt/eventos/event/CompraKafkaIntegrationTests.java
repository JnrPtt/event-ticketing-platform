package com.jnrptt.eventos.event;

import com.jnrptt.eventos.TestContainersConfiguration;
import com.jnrptt.eventos.dto.CompraRequest;
import com.jnrptt.eventos.dto.CompraResponse;
import com.jnrptt.eventos.exception.AforoInsuficienteException;
import com.jnrptt.eventos.model.Evento;
import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.model.TipoEntrada;
import com.jnrptt.eventos.model.TiposEntrada;
import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.repository.CompraRepository;
import com.jnrptt.eventos.repository.EventoRepository;
import com.jnrptt.eventos.repository.TipoEntradaRepository;
import com.jnrptt.eventos.repository.UsuarioRepository;
import com.jnrptt.eventos.service.CompraService;
import com.jnrptt.eventos.service.NotificacionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(TestContainersConfiguration.class)
class CompraKafkaIntegrationTests {

    private static final int AFORO = 10;
    private static final long ESPERA_MENSAJE_MS = 10_000;
    private static final long ESPERA_SIN_MENSAJE_MS = 2_000;

    @MockitoSpyBean
    private NotificacionService notificacionService;

    @Autowired
    private CompraService compraService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

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

    @BeforeEach
    void esperaAQueElConsumerTengaParticiones() {
        kafkaListenerEndpointRegistry.getListenerContainers()
                .forEach(container -> ContainerTestUtils.waitForAssignment(container, 3));
    }

    @Test
    void compraConfirmadaLlegaAlConsumerConLosDatosDeLaCompra() {
        var escenario = creaEscenario();

        CompraResponse compra = compraService.comprar(escenario.usuario().getId(),
                new CompraRequest(escenario.tipoEntrada().getId(), 2));

        verify(notificacionService, timeout(ESPERA_MENSAJE_MS)).enviarConfirmacion(argThat(evento ->
                evento.compraId().equals(compra.id())
                        && evento.usuarioId().equals(escenario.usuario().getId())
                        && evento.email().equals("ana@example.com")
                        && evento.nombreEvento().equals("Concierto")
                        && evento.tipoEntrada().equals("GENERAL")
                        && evento.cantidad() == 2
                        && evento.total().compareTo(new BigDecimal("50.00")) == 0));
    }

    @Test
    void aforoInsuficienteNoPublicaNada() {
        var escenario = creaEscenario();
        Long usuarioId = escenario.usuario().getId();

        assertThatThrownBy(() -> compraService.comprar(usuarioId,
                new CompraRequest(escenario.tipoEntrada().getId(), AFORO + 1)))
                .isInstanceOf(AforoInsuficienteException.class);

        verify(notificacionService, after(ESPERA_SIN_MENSAJE_MS).never())
                .enviarConfirmacion(argThat(evento -> evento.usuarioId().equals(usuarioId)));
    }

    @Test
    void rollbackDespuesDePublicarNoEnviaElMensaje() {
        long compraIdInexistente = 999_999L;
        var evento = new CompraConfirmadaEvent(compraIdInexistente, 999_999L, "rollback@example.com",
                "Concierto", "GENERAL", 1, new BigDecimal("25.00"), LocalDateTime.now());

        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(evento);
            status.setRollbackOnly();
        });

        verify(notificacionService, after(ESPERA_SIN_MENSAJE_MS).never())
                .enviarConfirmacion(argThat(recibido -> recibido.compraId().equals(compraIdInexistente)));
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
