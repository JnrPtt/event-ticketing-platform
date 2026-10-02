package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.EventoRequest;
import com.jnrptt.eventos.dto.TipoEntradaRequest;
import com.jnrptt.eventos.model.TiposEntrada;
import com.jnrptt.eventos.repository.EventoRepository;
import com.jnrptt.eventos.repository.TipoEntradaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EventoServiceIntegrationTests {

    @Autowired
    private EventoService eventoService;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private TipoEntradaRepository tipoEntradaRepository;

    @Test
    void creaActualizaYEliminaLasEntradasJuntoConElEvento() {
        var creado = eventoService.createEvento(evento("Concierto", List.of(
                entrada(TiposEntrada.GENERAL, "25.00", 100),
                entrada(TiposEntrada.VIP, "75.00", 20)
        )));

        assertThat(creado.getId()).isNotNull();
        assertThat(tipoEntradaRepository.count()).isEqualTo(2);

        eventoService.editEventoById(creado.getId(), evento("Concierto actualizado", List.of(
                entrada(TiposEntrada.GENERAL, "30.00", 80)
        )));

        assertThat(eventoService.getEventoById(creado.getId()).getNombre()).isEqualTo("Concierto actualizado");
        assertThat(tipoEntradaRepository.count()).isEqualTo(1);

        eventoService.deleteEventoById(creado.getId());

        assertThat(eventoRepository.count()).isZero();
        assertThat(tipoEntradaRepository.count()).isZero();
    }

    private EventoRequest evento(String nombre, List<TipoEntradaRequest> entradas) {
        return new EventoRequest(nombre, "Descripción", "2026-12-31", "Madrid", "Sala", entradas);
    }

    private TipoEntradaRequest entrada(TiposEntrada tipo, String precio, int aforo) {
        return new TipoEntradaRequest(tipo, new BigDecimal(precio), aforo);
    }
}
