package com.jnrptt.eventos.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompraConfirmadaPublisher {

    private final KafkaTemplate<String, CompraConfirmadaEvent> kafkaTemplate;

    @Value("${kafka.topics.compras-confirmadas}")
    private String topic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publicar(CompraConfirmadaEvent evento) {
        String clave = String.valueOf(evento.compraId());
        kafkaTemplate.send(topic, clave, evento)
                .whenComplete((resultado, error) -> {
                    if (error != null) {
                        log.error("Error al publicar evento de compra confirmada con id: {}", evento.compraId(), error);
                    } else {
                        log.info("Compra confirmada {} publicada en partición {}, offset {}", evento.compraId(), resultado.getRecordMetadata().partition(), resultado.getRecordMetadata().offset());
                    }
                });

    }
}
