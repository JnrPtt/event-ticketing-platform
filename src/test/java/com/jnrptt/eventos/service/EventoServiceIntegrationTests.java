package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.EventoRequest;
import com.jnrptt.eventos.dto.TipoEntradaRequest;
import com.jnrptt.eventos.model.TiposEntrada;
import com.jnrptt.eventos.repository.EventoRepository;
import com.jnrptt.eventos.repository.TipoEntradaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class EventoServiceIntegrationTests {

    @Autowired
    private EventoService eventoService;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private TipoEntradaRepository tipoEntradaRepository;

    @BeforeEach
    @AfterEach
    void limpiaEventos() {
        eventoRepository.deleteAll();
    }

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

    @Test
    void editarConservaLasEntradasVendidas() {
        var creado = eventoService.createEvento(evento("Concierto", List.of(
                entrada(TiposEntrada.GENERAL, "25.00", 100)
        )));
        var general = creado.getTipoEntrada().get(0);
        general.setAforoDisponible(70);
        tipoEntradaRepository.save(general);

        eventoService.editEventoById(creado.getId(), evento("Concierto", List.of(
                entrada(TiposEntrada.GENERAL, "30.00", 120)
        )));

        var editada = eventoService.getEventoById(creado.getId()).getTipoEntrada().get(0);
        assertThat(editada.getId()).isEqualTo(general.getId());
        assertThat(editada.getAforoTotal()).isEqualTo(120);
        assertThat(editada.getAforoDisponible()).isEqualTo(90);

        assertThatThrownBy(() -> eventoService.editEventoById(creado.getId(), evento("Concierto", List.of(
                entrada(TiposEntrada.GENERAL, "30.00", 20)
        )))).isInstanceOf(ResponseStatusException.class);

        eventoService.deleteEventoById(creado.getId());
    }

    @Test
    void filtraEventosPorCiudadYFecha(){
        eventoService.createEvento(new EventoRequest("Concierto Rock", "Concierto de rock en vivo",
                LocalDate.of(2026, 12, 31), "Sevilla", "Cartuja",
                List.of(entrada(TiposEntrada.GENERAL, "40.00", 200))));

        eventoService.createEvento(new EventoRequest("Festival de Jazz", "Festival de jazz al aire libre",
                LocalDate.of(2026, 12, 31), "Barcelona", "Parc del Fòrum",
                List.of(entrada(TiposEntrada.VIP, "90.00", 50))));

        eventoService.createEvento(new EventoRequest("Obra de Teatro", "Comedia clásica en dos actos",
                LocalDate.of(2026, 10, 24), "Sevilla", "Teatro Lope de Vega",
                List.of(entrada(TiposEntrada.GENERAL, "25.00", 150))));

        assertThat(eventoService.getAllEventos(null, null)).hasSize(3);
        assertThat(eventoService.getAllEventos("sevilla", null)).hasSize(2);
        assertThat(eventoService.getAllEventos(null, LocalDate.of(2026, 12, 31))).hasSize(2);
        assertThat(eventoService.getAllEventos("sevilla", LocalDate.of(2026, 12, 31))).hasSize(1);
    }

    private EventoRequest evento(String nombre, List<TipoEntradaRequest> entradas) {
        return new EventoRequest(nombre, "Descripción", LocalDate.of(2026, 12, 31), "Madrid", "Sala", entradas);
    }

    private TipoEntradaRequest entrada(TiposEntrada tipo, String precio, int aforo) {
        return new TipoEntradaRequest(tipo, new BigDecimal(precio), aforo);
    }
}
