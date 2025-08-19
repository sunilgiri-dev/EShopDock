package com.learn.inventoryservice.listener;

import com.learn.inventoryservice.events.OrderEvent;
import com.learn.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private final InventoryService inventoryService;

    @KafkaListener(topics = {"order-created", "order-cancelled"}, 
                   groupId = "inventory-service-order-group")
    public void handleOrderEvent(@Payload OrderEvent orderEvent,
                               @Header(KafkaHeaders.TOPIC) String topic,
                               @Header(KafkaHeaders.PARTITION_ID) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset,
                               Acknowledgment acknowledgment) {
        
        log.info("Received order event: {} from topic: {} partition: {} offset: {}", 
                orderEvent.getEventType(), topic, partition, offset);
        
        try {
            switch (orderEvent.getEventType()) {
                case OrderEvent.ORDER_CREATED -> handleOrderCreated(orderEvent);
                case OrderEvent.ORDER_CANCELLED -> handleOrderCancelled(orderEvent);
                default -> log.warn("Unknown order event type: {}", orderEvent.getEventType());
            }
            
            acknowledgment.acknowledge();
            log.info("Order event processed successfully: {}", orderEvent.getEventId());
            
        } catch (Exception e) {
            log.error("Error processing order event: {}", orderEvent.getEventId(), e);
            // Don't acknowledge on error - message will be reprocessed
        }
    }

    private void handleOrderCreated(OrderEvent orderEvent) {
        log.info("Processing order created event: {}", orderEvent.getOrderId());
        
        // Reduce inventory for each item in the order
        for (OrderEvent.OrderItem item : orderEvent.getItems()) {
            try {
                inventoryService.reduceInventory(item.getProductId(), item.getQuantity());
                log.info("Reduced inventory for product: {} by quantity: {}", 
                        item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Failed to reduce inventory for product: {} in order: {}", 
                         item.getProductId(), orderEvent.getOrderId(), e);
                // In a real system, you might want to implement compensation logic here
            }
        }
    }

    private void handleOrderCancelled(OrderEvent orderEvent) {
        log.info("Processing order cancelled event: {}", orderEvent.getOrderId());
        
        // Restore inventory for each item in the cancelled order
        for (OrderEvent.OrderItem item : orderEvent.getItems()) {
            try {
                inventoryService.restoreInventory(item.getProductId(), item.getQuantity());
                log.info("Restored inventory for product: {} by quantity: {}", 
                        item.getProductId(), item.getQuantity());
            } catch (Exception e) {
                log.error("Failed to restore inventory for product: {} in cancelled order: {}", 
                         item.getProductId(), orderEvent.getOrderId(), e);
            }
        }
    }
}