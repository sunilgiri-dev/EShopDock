package com.learn.inventoryservice.service;

import com.learn.inventoryservice.events.InventoryEvent;
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

    public void publishInventoryEvent(InventoryEvent inventoryEvent) {
        inventoryEvent.setEventId(UUID.randomUUID().toString());
        
        String topicName = getTopicName(inventoryEvent.getEventType());
        
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topicName, inventoryEvent.getProductId(), inventoryEvent);
        
        future.whenComplete((result, exception) -> {
            if (exception != null) {
                log.error("Failed to send inventory event: {} to topic: {}", inventoryEvent.getEventId(), topicName, exception);
            } else {
                log.info("Inventory event sent successfully: {} to topic: {} with offset: {}", 
                    inventoryEvent.getEventId(), topicName, result.getRecordMetadata().offset());
            }
        });
    }

    private String getTopicName(String eventType) {
        return switch (eventType) {
            case InventoryEvent.INVENTORY_UPDATED -> "inventory-updated";
            case InventoryEvent.INVENTORY_LOW_STOCK -> "inventory-low-stock";
            case InventoryEvent.INVENTORY_OUT_OF_STOCK -> "inventory-out-of-stock";
            case InventoryEvent.INVENTORY_RESTOCKED -> "inventory-restocked";
            default -> "inventory-events";
        };
    }
}