package com.example.nidar.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic sosAlertsTopic() {
        return TopicBuilder.name("sos-alerts")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic locationIncidentsTopic() {
        return TopicBuilder.name("location-incidents")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
