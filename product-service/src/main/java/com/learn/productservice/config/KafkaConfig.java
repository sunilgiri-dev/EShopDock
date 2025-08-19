package com.learn.productservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import jakarta.annotation.PostConstruct;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {
    
    @PostConstruct
    public void init() {
        log.info("KafkaConfig initialized - Kafka is enabled for product-service");
        System.out.println("KafkaConfig initialized - Kafka is enabled for product-service");
    }
}