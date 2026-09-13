package com.jnrptt.eventos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tipoentrada")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TipoEntrada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo",nullable = false)
    private TiposEntrada nombre;

    @Column(name = "precio", nullable = false)
    private BigDecimal precio;

    @Column(name = "aforo_total", nullable = false)
    private Integer aforoTotal;

    @Column(name = "aforo_disponible", nullable = false)
    private Integer aforoDisponible;

    @ManyToOne
    @JoinColumn(name = "evento_id")
    private Evento evento;
}

