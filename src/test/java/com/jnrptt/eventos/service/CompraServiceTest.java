package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.CompraRequest;
import com.jnrptt.eventos.event.CompraConfirmadaEvent;
import com.jnrptt.eventos.exception.AforoInsuficienteException;
import com.jnrptt.eventos.exception.CompraNotFoundException;
import com.jnrptt.eventos.exception.TransicionEstadoInvalidaException;
import com.jnrptt.eventos.model.Compra;
import com.jnrptt.eventos.model.EstadoCompra;
import com.jnrptt.eventos.model.Evento;
import com.jnrptt.eventos.model.Rol;
import com.jnrptt.eventos.model.TipoEntrada;
import com.jnrptt.eventos.model.TiposEntrada;
import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.repository.CompraRepository;
import com.jnrptt.eventos.repository.TipoEntradaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Test unitario de CompraService: las dependencias son mocks, así que se prueba solo la lógica del
// servicio (qué guarda, qué publica, qué bloquea y en qué orden) sin base de datos ni Kafka.
// La concurrencia real y el comportamiento de los bloqueos los cubren los tests de integración.
@ExtendWith(MockitoExtension.class)
class CompraServiceTest {

    private static final Long USUARIO_ID = 1L;
    private static final Long TIPO_ENTRADA_ID = 10L;
    private static final Long COMPRA_ID = 100L;

    @Mock
    private CompraRepository compraRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private TipoEntradaRepository tipoEntradaRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CompraService compraService;

    private Usuario usuario;
    private TipoEntrada tipoEntrada;

    @BeforeEach
    void preparaDatos() {
        usuario = new Usuario(USUARIO_ID, "Ana", "ana@example.com", "hash", Rol.USUARIO);
        var evento = new Evento(5L, "Concierto", "Concierto de prueba", LocalDate.now().plusDays(30),
                "Madrid", "WiZink Center", new ArrayList<>());
        tipoEntrada = new TipoEntrada(TIPO_ENTRADA_ID, TiposEntrada.GENERAL, new BigDecimal("25.00"), 10, 10, evento);
    }

    @Test
    void comprarDescuentaElAforoYGuardaUnaReservaPendienteSinPublicar() {
        when(usuarioService.obtenerPorId(USUARIO_ID)).thenReturn(usuario);
        when(tipoEntradaRepository.findByIdForUpdate(TIPO_ENTRADA_ID)).thenReturn(Optional.of(tipoEntrada));
        when(compraRepository.save(any(Compra.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var respuesta = compraService.comprar(USUARIO_ID, new CompraRequest(TIPO_ENTRADA_ID, 3));

        ArgumentCaptor<Compra> guardada = ArgumentCaptor.forClass(Compra.class);
        verify(compraRepository).save(guardada.capture());
        assertThat(guardada.getValue().getEstado()).isEqualTo(EstadoCompra.PENDIENTE);
        assertThat(guardada.getValue().getCantidad()).isEqualTo(3);
        assertThat(guardada.getValue().getPrecioUnitario()).isEqualByComparingTo("25.00");
        assertThat(guardada.getValue().getUsuario()).isSameAs(usuario);
        assertThat(tipoEntrada.getAforoDisponible()).isEqualTo(7);
        assertThat(respuesta.total()).isEqualByComparingTo("75.00");
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void comprarSinAforoSuficienteNoGuardaNadaNiTocaElAforo() {
        when(usuarioService.obtenerPorId(USUARIO_ID)).thenReturn(usuario);
        when(tipoEntradaRepository.findByIdForUpdate(TIPO_ENTRADA_ID)).thenReturn(Optional.of(tipoEntrada));

        assertThatThrownBy(() -> compraService.comprar(USUARIO_ID, new CompraRequest(TIPO_ENTRADA_ID, 11)))
                .isInstanceOf(AforoInsuficienteException.class);

        assertThat(tipoEntrada.getAforoDisponible()).isEqualTo(10);
        verify(compraRepository, never()).save(any());
    }

    @Test
    void pagarConfirmaLaCompraYPublicaElEventoConSusDatos() {
        var compra = compraEn(EstadoCompra.PENDIENTE);
        when(compraRepository.findByIdAndUsuarioIdForUpdate(COMPRA_ID, USUARIO_ID)).thenReturn(Optional.of(compra));

        var respuesta = compraService.pagar(USUARIO_ID, COMPRA_ID);

        assertThat(respuesta.estado()).isEqualTo(EstadoCompra.CONFIRMADA);
        ArgumentCaptor<Object> publicado = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(publicado.capture());
        assertThat(publicado.getValue()).isInstanceOfSatisfying(CompraConfirmadaEvent.class, evento -> {
            assertThat(evento.compraId()).isEqualTo(COMPRA_ID);
            assertThat(evento.email()).isEqualTo("ana@example.com");
            assertThat(evento.cantidad()).isEqualTo(2);
            assertThat(evento.total()).isEqualByComparingTo("50.00");
        });
    }

    @Test
    void pagarUnaCompraQueNoEsDelUsuarioLanza404YNoPublica() {
        when(compraRepository.findByIdAndUsuarioIdForUpdate(COMPRA_ID, USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compraService.pagar(USUARIO_ID, COMPRA_ID))
                .isInstanceOf(CompraNotFoundException.class);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void pagarUnaCompraYaConfirmadaNoPublicaOtraVez() {
        var compra = compraEn(EstadoCompra.CONFIRMADA);
        when(compraRepository.findByIdAndUsuarioIdForUpdate(COMPRA_ID, USUARIO_ID)).thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> compraService.pagar(USUARIO_ID, COMPRA_ID))
                .isInstanceOf(TransicionEstadoInvalidaException.class);

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void cancelarBloqueaElTipoDeEntradaYDevuelveElAforo() {
        tipoEntrada.setAforoDisponible(8);
        var compra = compraEn(EstadoCompra.CONFIRMADA);
        when(compraRepository.findByIdAndUsuarioIdForUpdate(COMPRA_ID, USUARIO_ID)).thenReturn(Optional.of(compra));
        when(tipoEntradaRepository.findByIdForUpdate(TIPO_ENTRADA_ID)).thenReturn(Optional.of(tipoEntrada));

        var respuesta = compraService.cancelar(USUARIO_ID, COMPRA_ID);

        assertThat(respuesta.estado()).isEqualTo(EstadoCompra.CANCELADA);
        assertThat(tipoEntrada.getAforoDisponible()).isEqualTo(10);
    }

    // Una cancelación repetida se rechaza antes de bloquear el tipo de entrada,
    // que es la fila por la que compiten todas las compras.
    @Test
    void cancelarUnaCompraYaCanceladaNoLlegaABloquearElTipoDeEntrada() {
        var compra = compraEn(EstadoCompra.CANCELADA);
        when(compraRepository.findByIdAndUsuarioIdForUpdate(COMPRA_ID, USUARIO_ID)).thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> compraService.cancelar(USUARIO_ID, COMPRA_ID))
                .isInstanceOf(TransicionEstadoInvalidaException.class);

        verify(tipoEntradaRepository, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void listarUsaElOrdenFijoAunqueElClientePidaOtro() {
        when(compraRepository.findByUsuarioId(eq(USUARIO_ID), any(Pageable.class))).thenReturn(Page.empty());

        compraService.listarDelUsuario(USUARIO_ID, PageRequest.of(2, 15, Sort.by("usuario.password")));

        ArgumentCaptor<Pageable> pedida = ArgumentCaptor.forClass(Pageable.class);
        verify(compraRepository).findByUsuarioId(eq(USUARIO_ID), pedida.capture());
        assertThat(pedida.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pedida.getValue().getPageSize()).isEqualTo(15);
        assertThat(pedida.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "fecha", "id"));
    }

    private Compra compraEn(EstadoCompra estado) {
        return new Compra(COMPRA_ID, 2, estado, LocalDateTime.now(), new BigDecimal("25.00"), usuario, tipoEntrada);
    }
}
