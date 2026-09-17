package com.jnrptt.eventos.controller;

import com.jnrptt.eventos.dto.EventoRequest;
import com.jnrptt.eventos.model.Evento;
import com.jnrptt.eventos.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eventos")
public class EventoController {
    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping("/{id}")
    public Evento getEventoById(@PathVariable Long id) {
        return eventoService.getEventoById(id);
    }

    @PostMapping
    public void createEvento(@Valid @RequestBody EventoRequest eventoRequest) {
        eventoService.createEvento(eventoRequest);
    }
}
