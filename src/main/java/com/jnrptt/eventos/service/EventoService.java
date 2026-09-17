package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.EventoRequest;
import com.jnrptt.eventos.model.Evento;
import com.jnrptt.eventos.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventoService {
    private final EventoRepository eventoRepository;

    public void createEvento(EventoRequest eventoRequest) {
        eventoRepository.save(eventoRequest.toEvento());
    }

    public Evento getEventoById(Long id) {
        return eventoRepository.findById(id).orElse(null);
    }

}
