package com.learn.productservice.service;

import com.learn.productservice.events.ProductEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("EventPublisherService initialized with KafkaTemplate: {}", kafkaTemplate);
        System.out.println("EventPublisherService initialized with KafkaTemplate: " + kafkaTemplate);
    }

    public void publishProductEvent(ProductEvent productEvent) {
        try {
            log.debug("Starting to publish product event: {}", productEvent);
            productEvent.setEventId(UUID.randomUUID().toString());
            
            String topicName = getTopicName(productEvent.getEventType());
            log.info("Publishing product event {} to topic: {}", productEvent.getEventId(), topicName);
            
            CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topicName, productEvent.getProductId(), productEvent);
            
            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to send product event: {} to topic: {}", productEvent.getEventId(), topicName, exception);
                } else {
                    log.info("Product event sent successfully: {} to topic: {} with offset: {}", 
                        productEvent.getEventId(), topicName, result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            log.error("Exception occurred while publishing product event", e);
            throw e;
        }
    }

    private String getTopicName(String eventType) {
        return switch (eventType) {
            case ProductEvent.PRODUCT_CREATED -> "product-created";
            case ProductEvent.PRODUCT_UPDATED -> "product-updated";
            case ProductEvent.PRODUCT_DELETED -> "product-deleted";
            default -> "product-events";
        };
    }
}