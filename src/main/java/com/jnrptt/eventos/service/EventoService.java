package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.EventoRequest;
import com.jnrptt.eventos.model.Evento;
import com.jnrptt.eventos.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventoService {
    private final EventoRepository eventoRepository;

    @Transactional
    public Evento createEvento(EventoRequest eventoRequest) {
        return eventoRepository.save(eventoRequest.toEvento());
    }

    @Transactional(readOnly = true)
    public Evento getEventoById(Long id) {
        return eventoRepository.findWithTipoEntradaById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<Evento> getAllEventos() {
        return eventoRepository.findAll();
    }

    @Transactional
    public void deleteEventoById(Long id) {
        if (!eventoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento no encontrado");
        }
        eventoRepository.deleteById(id);
    }

    @Transactional
    public Evento editEventoById(Long id, EventoRequest eventoRequest) {
        Evento replacement = eventoRequest.toEvento();
        Evento evento = getEventoById(id);
        evento.setNombre(replacement.getNombre());
        evento.setDescripcion(replacement.getDescripcion());
        evento.setFecha(replacement.getFecha());
        evento.setCiudad(replacement.getCiudad());
        evento.setLugar(replacement.getLugar());
        evento.getTipoEntrada().clear();
        replacement.getTipoEntrada().forEach(entrada -> {
            entrada.setEvento(evento);
            evento.getTipoEntrada().add(entrada);
        });
        return evento;
    }

}
