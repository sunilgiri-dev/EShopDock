package com.itransform.orderservice.service;

import com.itransform.orderservice.events.OrderEvent;
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

    public void publishOrderEvent(OrderEvent orderEvent) {
        orderEvent.setEventId(UUID.randomUUID().toString());
        
        String topicName = getTopicName(orderEvent.getEventType());
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topicName, orderEvent.getOrderId(), orderEvent);
        
        future.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Failed to send order event: {} to topic: {}", orderEvent.getEventId(), topicName, exception);
            } else {
                log.info("Order event sent successfully: {} to topic: {} with offset: {}", 
                    orderEvent.getEventId(), topicName, result.getRecordMetadata().offset());
            }
        });
    }

    private String getTopicName(String eventType) {
        return switch (eventType) {
            case OrderEvent.ORDER_CREATED -> "order-created";
            case OrderEvent.ORDER_UPDATED -> "order-updated";
            case OrderEvent.ORDER_CANCELLED -> "order-cancelled";
            case OrderEvent.ORDER_COMPLETED -> "order-completed";
            default -> "order-events";
        };
    }
}