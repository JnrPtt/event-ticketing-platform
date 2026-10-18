package com.jnrptt.eventos.event;

import com.jnrptt.eventos.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionConsumer {
    private final NotificacionService notificacionService;

    @KafkaListener(topics = "${kafka.topics.compras-confirmadas}", groupId = "notificaciones")
    public void consumir(CompraConfirmadaEvent evento) {
        log.debug("Mensaje recibido con id {}", evento.compraId());
        notificacionService.enviarConfirmacion(evento);
    }
}
