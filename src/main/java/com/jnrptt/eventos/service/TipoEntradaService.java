package com.jnrptt.eventos.service;

import com.jnrptt.eventos.model.TipoEntrada;
import com.jnrptt.eventos.repository.TipoEntradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TipoEntradaService {
    private final TipoEntradaRepository tipoEntradaRepository;
    private final EventoService eventoService;

    public void saveTipoEntrada(TipoEntrada tipoEntrada) {
        tipoEntradaRepository.save(tipoEntrada);
    }


}
