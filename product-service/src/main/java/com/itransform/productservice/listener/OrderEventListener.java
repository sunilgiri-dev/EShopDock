package com.itransform.productservice.listener;

import com.itransform.productservice.events.OrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventListener {

    @KafkaListener(topics = {"order-created", "order-updated", "order-cancelled", "order-completed"}, 
                   groupId = "product-service-order-group")
    public void handleOrderEvent(@Payload OrderEvent orderEvent,
                               @Header(KafkaHeaders.TOPIC) String topic,
                               @Header(KafkaHeaders.PARTITION) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset,
                               Acknowledgment acknowledgment) {
        
        log.info("Received order event: {} from topic: {} partition: {} offset: {}", 
                orderEvent.getEventType(), topic, partition, offset);
        
        try {
            switch (orderEvent.getEventType()) {
                case OrderEvent.ORDER_CREATED -> handleOrderCreated(orderEvent);
                case OrderEvent.ORDER_UPDATED -> handleOrderUpdated(orderEvent);
                case OrderEvent.ORDER_CANCELLED -> handleOrderCancelled(orderEvent);
                case OrderEvent.ORDER_COMPLETED -> handleOrderCompleted(orderEvent);
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
        // TODO: Update product analytics, track popular products
        for (OrderEvent.OrderItem item : orderEvent.getItems()) {
            log.info("Product {} ordered with quantity {}", item.getProductId(), item.getQuantity());
        }
    }

    private void handleOrderUpdated(OrderEvent orderEvent) {
        log.info("Processing order updated event: {}", orderEvent.getOrderId());
        // TODO: Handle order updates if needed
    }

    private void handleOrderCancelled(OrderEvent orderEvent) {
        log.info("Processing order cancelled event: {}", orderEvent.getOrderId());
        // TODO: Update product analytics for cancelled orders
    }

    private void handleOrderCompleted(OrderEvent orderEvent) {
        log.info("Processing order completed event: {}", orderEvent.getOrderId());
        // TODO: Update product sales statistics
    }
}