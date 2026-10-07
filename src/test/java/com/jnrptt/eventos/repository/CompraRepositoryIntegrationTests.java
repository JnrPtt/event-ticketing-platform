package com.jnrptt.eventos.repository;

import com.jnrptt.eventos.model.Compra;
import com.jnrptt.eventos.model.EstadoCompra;
import com.jnrptt.eventos.model.Evento;
import com.jnrptt.eventos.model.TipoEntrada;
import com.jnrptt.eventos.model.TiposEntrada;
import com.jnrptt.eventos.model.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CompraRepositoryIntegrationTests {

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
    void guardaUnaCompraPendienteYLaRecuperaPorId() {
        var usuario = usuarioRepository.save(new Usuario(null, "Ana", "ana@example.com"));
        var tipoEntrada = tipoEntradaRepository.save(tipoEntrada(eventoRepository.save(evento())));

        var guardada = compraRepository.save(new Compra(
                null, 2, EstadoCompra.PENDIENTE, LocalDateTime.now(), new BigDecimal("25.00"), usuario, tipoEntrada));

        var encontrada = compraRepository.findById(guardada.getId());

        assertThat(encontrada).isPresent();
        assertThat(encontrada.get().getEstado()).isEqualTo(EstadoCompra.PENDIENTE);
        assertThat(encontrada.get().getCantidad()).isEqualTo(2);
        assertThat(encontrada.get().getUsuario().getId()).isEqualTo(usuario.getId());
        assertThat(encontrada.get().getTipoEntrada().getId()).isEqualTo(tipoEntrada.getId());
    }

    private Evento evento() {
        return new Evento(null, "Concierto", "Concierto de prueba", LocalDate.now().plusDays(30),
                "Madrid", "WiZink Center", new ArrayList<>());
    }

    private TipoEntrada tipoEntrada(Evento evento) {
        return new TipoEntrada(null, TiposEntrada.GENERAL, new BigDecimal("25.00"), 100, 100, evento);
    }
}
