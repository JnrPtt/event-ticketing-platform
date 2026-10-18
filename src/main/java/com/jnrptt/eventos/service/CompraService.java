package com.jnrptt.eventos.service;

import com.jnrptt.eventos.dto.CompraRequest;
import com.jnrptt.eventos.dto.CompraResponse;
import com.jnrptt.eventos.event.CompraConfirmadaEvent;
import com.jnrptt.eventos.exception.AforoInsuficienteException;
import com.jnrptt.eventos.exception.CompraNotFoundException;
import com.jnrptt.eventos.exception.TipoEntradaNotFoundException;
import com.jnrptt.eventos.model.Compra;
import com.jnrptt.eventos.model.EstadoCompra;
import com.jnrptt.eventos.model.TipoEntrada;
import com.jnrptt.eventos.model.Usuario;
import com.jnrptt.eventos.repository.CompraRepository;
import com.jnrptt.eventos.repository.TipoEntradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CompraService {
    private static final Sort ORDEN_COMPRAS = Sort.by(Sort.Direction.DESC, "fecha", "id");

    private final CompraRepository compraRepository;
    private final UsuarioService usuarioService;
    private final TipoEntradaRepository tipoEntradaRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CompraResponse comprar(Long usuarioId, CompraRequest compraRequest) {
        Usuario usuario = usuarioService.obtenerPorId(usuarioId);
        // Bloqueo pesimista (SELECT ... FOR UPDATE): las compras de un mismo tipo de entrada
        // compiten mucho por la misma fila, y con bloqueo optimista la mayoría fallarían y habría
        // que reintentarlas. Así se serializan en la BD y cada una ve el aforo ya descontado.
        TipoEntrada tipoEntrada = tipoEntradaRepository.findByIdForUpdate(compraRequest.tipoEntradaId())
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
                EstadoCompra.PENDIENTE,
                LocalDateTime.now(),
                tipoEntrada.getPrecio(),
                usuario,
                tipoEntrada
        );
        Compra guardada = compraRepository.save(compra);
        return CompraResponse.from(guardada);
    }

    @Transactional
    public CompraResponse pagar(Long usuarioId, Long compraId) {
        Compra compra = compraRepository.findByIdAndUsuarioIdForUpdate(compraId, usuarioId)
                .orElseThrow(() -> new CompraNotFoundException(compraId));

        compra.pagar();
        eventPublisher.publishEvent(CompraConfirmadaEvent.from(compra));
        return CompraResponse.from(compra);
    }

    @Transactional
    public CompraResponse cancelar(Long usuarioId, Long compraId) {
        Compra compra = compraRepository.findByIdAndUsuarioIdForUpdate(compraId, usuarioId)
                .orElseThrow(() -> new CompraNotFoundException(compraId));

        compra.cancelar();

        Long tipoEntradaId = compra.getTipoEntrada().getId();
        TipoEntrada tipoEntrada = tipoEntradaRepository.findByIdForUpdate(tipoEntradaId)
                .orElseThrow(() -> new TipoEntradaNotFoundException(tipoEntradaId));
        tipoEntrada.setAforoDisponible(tipoEntrada.getAforoDisponible() + compra.getCantidad());
        return CompraResponse.from(compra);
    }

    @Transactional(readOnly = true)
    public Page<CompraResponse> listarDelUsuario(Long usuarioId, Pageable pageable) {
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEN_COMPRAS);
        return compraRepository.findByUsuarioId(usuarioId, pagina).map(CompraResponse::from);
    }
}
