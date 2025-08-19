package com.learn.orderservice.listener;

import com.learn.orderservice.events.ProductEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ProductEventListener {

    @KafkaListener(topics = {"product-created", "product-updated", "product-deleted"}, 
                   groupId = "order-service-product-group")
    public void handleProductEvent(@Payload ProductEvent productEvent,
                                 @Header(KafkaHeaders.TOPIC) String topic,
                                 @Header(KafkaHeaders.PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset,
                                 Acknowledgment acknowledgment) {
        
        log.info("Received product event: {} from topic: {} partition: {} offset: {}", 
                productEvent.getEventType(), topic, partition, offset);
        
        try {
            switch (productEvent.getEventType()) {
                case ProductEvent.PRODUCT_CREATED -> handleProductCreated(productEvent);
                case ProductEvent.PRODUCT_UPDATED -> handleProductUpdated(productEvent);
                case ProductEvent.PRODUCT_DELETED -> handleProductDeleted(productEvent);
                default -> log.warn("Unknown product event type: {}", productEvent.getEventType());
            }
            
            acknowledgment.acknowledge();
            log.info("Product event processed successfully: {}", productEvent.getEventId());
            
        } catch (Exception e) {
            log.error("Error processing product event: {}", productEvent.getEventId(), e);
            // Don't acknowledge on error - message will be reprocessed
        }
    }

    private void handleProductCreated(ProductEvent productEvent) {
        log.info("Processing product created event for product: {}", productEvent.getProductId());
        // TODO: Update local product cache or perform any necessary actions
        // For example, update product pricing in order calculations
    }

    private void handleProductUpdated(ProductEvent productEvent) {
        log.info("Processing product updated event for product: {}", productEvent.getProductId());
        // TODO: Update local product information
        // For example, update product price for future orders
    }

    private void handleProductDeleted(ProductEvent productEvent) {
        log.info("Processing product deleted event for product: {}", productEvent.getProductId());
        // TODO: Handle product deletion
        // For example, mark product as unavailable for new orders
    }
}