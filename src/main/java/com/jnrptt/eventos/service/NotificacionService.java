package com.jnrptt.eventos.service;

import com.jnrptt.eventos.event.CompraConfirmadaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificacionService {
    public void enviarConfirmacion(CompraConfirmadaEvent evento) {
        log.info("Enviando email a {} de {} entradas {} para {} (total {})", evento.email(), evento.cantidad(), evento.tipoEntrada(), evento.nombreEvento(), evento.total());
    }
}
