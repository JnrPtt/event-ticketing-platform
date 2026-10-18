package com.jnrptt.eventos.model;

import com.jnrptt.eventos.exception.TransicionEstadoInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Test unitario puro: las transiciones viven en la entidad, así que no hace falta Spring ni base de datos.
class CompraTest {

    @Test
    void pagarUnaCompraPendienteLaConfirma() {
        Compra compra = compraEn(EstadoCompra.PENDIENTE);

        compra.pagar();

        assertThat(compra.getEstado()).isEqualTo(EstadoCompra.CONFIRMADA);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCompra.class, names = {"CONFIRMADA", "CANCELADA"})
    void pagarDesdeOtroEstadoLanzaExcepcionYNoCambiaElEstado(EstadoCompra estado) {
        Compra compra = compraEn(estado);

        assertThatThrownBy(compra::pagar)
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessage("No se puede pagar una compra en estado " + estado);

        assertThat(compra.getEstado()).isEqualTo(estado);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCompra.class, names = {"PENDIENTE", "CONFIRMADA"})
    void cancelarUnaCompraPendienteOConfirmadaLaCancela(EstadoCompra estado) {
        Compra compra = compraEn(estado);

        compra.cancelar();

        assertThat(compra.getEstado()).isEqualTo(EstadoCompra.CANCELADA);
    }

    @Test
    void cancelarUnaCompraCanceladaLanzaExcepcion() {
        Compra compra = compraEn(EstadoCompra.CANCELADA);

        assertThatThrownBy(compra::cancelar)
                .isInstanceOf(TransicionEstadoInvalidaException.class)
                .hasMessage("No se puede cancelar una compra en estado CANCELADA");

        assertThat(compra.getEstado()).isEqualTo(EstadoCompra.CANCELADA);
    }

    private static Compra compraEn(EstadoCompra estado) {
        return new Compra(1L, 2, estado, LocalDateTime.now(), new BigDecimal("25.00"), null, null);
    }
}
