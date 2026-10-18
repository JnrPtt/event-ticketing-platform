package com.jnrptt.eventos.model;

import com.jnrptt.eventos.exception.TransicionEstadoInvalidaException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "compras")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    @Setter(AccessLevel.NONE)
    private EstadoCompra estado;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "precio_unitario", nullable = false)
    private BigDecimal precioUnitario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_entrada_id", nullable = false)
    private TipoEntrada tipoEntrada;

    public void pagar() {
        if (estado != EstadoCompra.PENDIENTE) {
            throw new TransicionEstadoInvalidaException("pagar", estado);
        }
        estado = EstadoCompra.CONFIRMADA;
    }

    public void cancelar() {
        if (estado == EstadoCompra.CANCELADA) {
            throw new TransicionEstadoInvalidaException("cancelar", estado);
        }
        estado = EstadoCompra.CANCELADA;
    }
}
