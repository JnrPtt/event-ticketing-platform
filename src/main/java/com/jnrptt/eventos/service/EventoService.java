package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.EventoRequest;
import com.jnrptt.eventos.exception.AforoMenorQueVendidasException;
import com.jnrptt.eventos.exception.EventoConComprasException;
import com.jnrptt.eventos.exception.EventoNotFoundException;
import com.jnrptt.eventos.exception.TipoEntradaConComprasException;
import com.jnrptt.eventos.exception.TipoEntradaRepetidoException;
import com.jnrptt.eventos.model.Evento;
import com.jnrptt.eventos.model.TipoEntrada;
import com.jnrptt.eventos.model.TiposEntrada;
import com.jnrptt.eventos.repository.CompraRepository;
import com.jnrptt.eventos.repository.EventoRepository;
import com.jnrptt.eventos.repository.EventoSpecifications;
import com.jnrptt.eventos.repository.TipoEntradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final TipoEntradaRepository tipoEntradaRepository;
    private final CompraRepository compraRepository;

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
    public List<Evento> getAllEventos(String ciudad, LocalDate fecha) {
        return eventoRepository.findAll(Specification.allOf(
                EventoSpecifications.porCiudad(ciudad),
                EventoSpecifications.porFecha(fecha)));
    }

    @Transactional
    public void deleteEventoById(Long id) {
        if (!eventoRepository.existsById(id)) {
            throw new EventoNotFoundException(id);
        }
        tipoEntradaRepository.findByEventoIdForUpdate(id);
        if (compraRepository.existsByTipoEntradaEventoId(id)) {
            throw new EventoConComprasException(id);
        }
        eventoRepository.deleteById(id);
    }

    @Transactional
    public Evento editEventoById(Long id, EventoRequest eventoRequest) {
        validarTiposEntradaUnicos(eventoRequest);
        Evento replacement = eventoRequest.toEvento();
        tipoEntradaRepository.findByEventoIdForUpdate(id);
        Evento evento = getEventoById(id);
        Set<TiposEntrada> tiposNuevos = replacement.getTipoEntrada().stream()
                .map(TipoEntrada::getNombre)
                .collect(Collectors.toSet());
        evento.getTipoEntrada().stream()
                .filter(tipo -> !tiposNuevos.contains(tipo.getNombre()))
                .filter(tipo -> compraRepository.existsByTipoEntradaId(tipo.getId()))
                .findFirst()
                .ifPresent(tipo -> {
                    throw new TipoEntradaConComprasException(tipo.getNombre());
                });
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
                throw new AforoMenorQueVendidasException(nueva.getNombre(), vendidas);
            }
            actual.setPrecio(nueva.getPrecio());
            actual.setAforoTotal(nueva.getAforoTotal());
            actual.setAforoDisponible(nueva.getAforoTotal() - vendidas);
            evento.getTipoEntrada().add(actual);
        });
        return evento;
    }

    private void validarTiposEntradaUnicos(EventoRequest eventoRequest) {
        Set<TiposEntrada> vistos = EnumSet.noneOf(TiposEntrada.class);
        eventoRequest.tipoEntrada().forEach(entrada -> {
            if (!vistos.add(entrada.nombre())) {
                throw new TipoEntradaRepetidoException(entrada.nombre());
            }
        });
    }

}
