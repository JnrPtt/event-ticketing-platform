package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.EventoRequest;
import com.jnrptt.eventos.exception.EventoNotFoundException;
import com.jnrptt.eventos.model.Evento;
import com.jnrptt.eventos.model.TipoEntrada;
import com.jnrptt.eventos.model.TiposEntrada;
import com.jnrptt.eventos.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventoService {
    private final EventoRepository eventoRepository;

    @Transactional
    public Evento createEvento(EventoRequest eventoRequest) {
        validarTiposEntradaUnicos(eventoRequest);
        return eventoRepository.save(eventoRequest.toEvento());
    }

    @Transactional(readOnly = true)
    public Evento getEventoById(Long id) {
        return eventoRepository.findWithTipoEntradaById(id)
                .orElseThrow(() -> new EventoNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Evento> getAllEventos() {
        return eventoRepository.findAll();
    }

    @Transactional
    public void deleteEventoById(Long id) {
        if (!eventoRepository.existsById(id)) {
            throw new EventoNotFoundException(id);
        }
        eventoRepository.deleteById(id);
    }

    @Transactional
    public Evento editEventoById(Long id, EventoRequest eventoRequest) {
        validarTiposEntradaUnicos(eventoRequest);
        Evento replacement = eventoRequest.toEvento();
        Evento evento = getEventoById(id);
        evento.setNombre(replacement.getNombre());
        evento.setDescripcion(replacement.getDescripcion());
        evento.setFecha(replacement.getFecha());
        evento.setCiudad(replacement.getCiudad());
        evento.setLugar(replacement.getLugar());
        Map<TiposEntrada, TipoEntrada> existentes = evento.getTipoEntrada().stream()
                .collect(Collectors.toMap(TipoEntrada::getNombre, Function.identity(), (a, b) -> a));
        evento.getTipoEntrada().clear();
        replacement.getTipoEntrada().forEach(nueva -> {
            TipoEntrada actual = existentes.remove(nueva.getNombre());
            if (actual == null) {
                nueva.setEvento(evento);
                evento.getTipoEntrada().add(nueva);
                return;
            }
            int vendidas = actual.getAforoTotal() - actual.getAforoDisponible();
            if (nueva.getAforoTotal() < vendidas) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El aforo de " + nueva.getNombre() + " no puede ser menor que las entradas vendidas (" + vendidas + ")");
            }
            actual.setPrecio(nueva.getPrecio());
            actual.setAforoTotal(nueva.getAforoTotal());
            actual.setAforoDisponible(nueva.getAforoTotal() - vendidas);
            evento.getTipoEntrada().add(actual);
        });
        existentes.values().forEach(eliminada -> {
            int vendidas = eliminada.getAforoTotal() - eliminada.getAforoDisponible();
            if (vendidas > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede eliminar " + eliminada.getNombre() + " porque tiene entradas vendidas (" + vendidas + ")");
            }
        });
        return evento;
    }

    private void validarTiposEntradaUnicos(EventoRequest eventoRequest) {
        Set<TiposEntrada> vistos = EnumSet.noneOf(TiposEntrada.class);
        eventoRequest.tipoEntrada().forEach(entrada -> {
            if (!vistos.add(entrada.nombre())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El tipo de entrada " + entrada.nombre() + " está repetido");
            }
        });
    }

}
