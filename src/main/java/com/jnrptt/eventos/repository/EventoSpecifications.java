package com.jnrptt.eventos.repository;

import com.jnrptt.eventos.model.Evento;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class EventoSpecifications {

    public static Specification<Evento> porCiudad(String ciudad) {
        if (ciudad == null || ciudad.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(criteriaBuilder.lower(root.get("ciudad")), ciudad.toLowerCase());
    }

    public static Specification<Evento> porFecha(LocalDate fecha) {
        if (fecha == null) {
            return Specification.unrestricted();
        }
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("fecha"), fecha);
    }
}
