package com.jnrptt.eventos.controller;

import com.jnrptt.eventos.dto.EventoRequest;
import com.jnrptt.eventos.dto.EventoResponse;
import com.jnrptt.eventos.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/eventos")
public class EventoController {
    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping("/{id}")
    public EventoResponse getEventoById(@PathVariable Long id) {
        return EventoResponse.from(eventoService.getEventoById(id));
    }

    @GetMapping
    public List<EventoResponse> getAllEventos(
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return eventoService.getAllEventos(ciudad, fecha).stream().map(EventoResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<EventoResponse> createEvento(@Valid @RequestBody EventoRequest eventoRequest) {
        EventoResponse response = EventoResponse.from(eventoService.createEvento(eventoRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public EventoResponse editEvento(@PathVariable Long id, @Valid @RequestBody EventoRequest eventoRequest) {
        return EventoResponse.from(eventoService.editEventoById(id, eventoRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvento(@PathVariable Long id) {
        eventoService.deleteEventoById(id);
        return ResponseEntity.noContent().build();
    }
}
