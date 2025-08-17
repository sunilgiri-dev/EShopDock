package com.itransform.orderservice.listener;

import com.itransform.orderservice.events.InventoryEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InventoryEventListener {

    @KafkaListener(topics = {"inventory-updated", "inventory-low-stock", "inventory-out-of-stock", "inventory-restocked"}, 
                   groupId = "order-service-inventory-group")
    public void handleInventoryEvent(@Payload InventoryEvent inventoryEvent,
                                   @Header(KafkaHeaders.TOPIC) String topic,
                                   @Header(KafkaHeaders.PARTITION) int partition,
                                   @Header(KafkaHeaders.OFFSET) long offset,
                                   Acknowledgment acknowledgment) {
        
        log.info("Received inventory event: {} from topic: {} partition: {} offset: {}", 
                inventoryEvent.getEventType(), topic, partition, offset);
        
        try {
            switch (inventoryEvent.getEventType()) {
                case InventoryEvent.INVENTORY_UPDATED -> handleInventoryUpdated(inventoryEvent);
                case InventoryEvent.INVENTORY_LOW_STOCK -> handleLowStock(inventoryEvent);
                case InventoryEvent.INVENTORY_OUT_OF_STOCK -> handleOutOfStock(inventoryEvent);
                case InventoryEvent.INVENTORY_RESTOCKED -> handleRestocked(inventoryEvent);
                default -> log.warn("Unknown inventory event type: {}", inventoryEvent.getEventType());
            }
            
            acknowledgment.acknowledge();
            log.info("Inventory event processed successfully: {}", inventoryEvent.getEventId());
            
        } catch (Exception e) {
            log.error("Error processing inventory event: {}", inventoryEvent.getEventId(), e);
            // Don't acknowledge on error - message will be reprocessed
        }
    }

    private void handleInventoryUpdated(InventoryEvent inventoryEvent) {
        log.info("Processing inventory updated event for product: {}, new quantity: {}", 
                inventoryEvent.getProductId(), inventoryEvent.getQuantity());
        // TODO: Update product availability for order processing
    }

    private void handleLowStock(InventoryEvent inventoryEvent) {
        log.warn("Low stock alert for product: {}, remaining quantity: {}", 
                inventoryEvent.getProductId(), inventoryEvent.getQuantity());
        // TODO: Potentially send notifications or adjust order processing rules
    }

    private void handleOutOfStock(InventoryEvent inventoryEvent) {
        log.warn("Out of stock for product: {}", inventoryEvent.getProductId());
        // TODO: Disable product for new orders, handle existing orders
    }

    private void handleRestocked(InventoryEvent inventoryEvent) {
        log.info("Product restocked: {}, new quantity: {}", 
                inventoryEvent.getProductId(), inventoryEvent.getQuantity());
        // TODO: Re-enable product for orders if it was disabled
    }
}