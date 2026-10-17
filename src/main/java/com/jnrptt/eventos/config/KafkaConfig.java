package com.jnrptt.eventos.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic comprasConfirmadasTopic(@Value("${kafka.topics.compras-confirmadas}") String nombre) {
        return TopicBuilder.name(nombre)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
