package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.CompraRequest;
import com.jnrptt.eventos.dto.CompraResponse;
import com.jnrptt.eventos.exception.AforoInsuficienteException;
import com.jnrptt.eventos.exception.TipoEntradaNotFoundException;
import com.jnrptt.eventos.model.Compra;
import com.jnrptt.eventos.model.EstadoCompra;
import com.jnrptt.eventos.model.TipoEntrada;
import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.repository.CompraRepository;
import com.jnrptt.eventos.repository.TipoEntradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CompraService {
    private final CompraRepository compraRepository;
    private final UsuarioService usuarioService;
    private final TipoEntradaRepository tipoEntradaRepository;

    @Transactional
    public CompraResponse comprar(CompraRequest compraRequest) {
        Usuario usuario = usuarioService.obtenerPorId(compraRequest.usuarioId());
        TipoEntrada tipoEntrada = tipoEntradaRepository.findById(compraRequest.tipoEntradaId())
                .orElseThrow(() -> new TipoEntradaNotFoundException(compraRequest.tipoEntradaId()));

        int cantidad = compraRequest.cantidad();
        int disponibles = tipoEntrada.getAforoDisponible();
        if (disponibles < cantidad) {
            throw new AforoInsuficienteException(cantidad, disponibles);
        }

        tipoEntrada.setAforoDisponible(disponibles - cantidad);

        Compra compra = new Compra(
                null,
                cantidad,
                EstadoCompra.CONFIRMADA,
                LocalDateTime.now(),
                tipoEntrada.getPrecio(),
                usuario,
                tipoEntrada
        );
        Compra guardada = compraRepository.save(compra);
        return CompraResponse.from(guardada);
    }
}
