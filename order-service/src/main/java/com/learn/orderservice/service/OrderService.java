package com.learn.orderservice.service;

import com.learn.orderservice.dto.*;
import com.learn.orderservice.dto.OrderItemDto;
import com.learn.orderservice.dto.OrderRequest;
import com.learn.orderservice.dto.OrderResponse;
import com.learn.orderservice.events.OrderEvent;
import com.learn.orderservice.exception.ResourceNotFoundException;
import com.learn.orderservice.model.*;
import com.learn.orderservice.model.Order;
import com.learn.orderservice.model.OrderItem;
import com.learn.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventPublisherService eventPublisherService;

    public OrderResponse createOrder(OrderRequest request, String username) {
        List<OrderItem> items = request.getItems().stream()
                .map(i -> OrderItem.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .build())
                .collect(Collectors.toList());

        Order order = Order.builder()
                .items(items)
                .status("CREATED")
                .username(username)
                .build();

        Order saved = orderRepository.save(order);
        
        // Publish order created event
        publishOrderCreatedEvent(saved);
        
        log.info("Order created successfully: {}", saved.getId());
        return mapToResponse(saved);
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<OrderResponse> getOrdersForUser(String username) {
        return orderRepository.findByUsername(username).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(String id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    public OrderResponse updateOrderStatus(String orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        
        String previousStatus = order.getStatus();
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        
        // Publish order updated event
        publishOrderUpdatedEvent(saved, previousStatus);
        
        log.info("Order status updated: {} from {} to {}", orderId, previousStatus, status);
        return mapToResponse(saved);
    }

    public OrderResponse cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        
        order.setStatus("CANCELLED");
        Order saved = orderRepository.save(order);
        
        // Publish order cancelled event
        publishOrderCancelledEvent(saved);
        
        log.info("Order cancelled: {}", orderId);
        return mapToResponse(saved);
    }

    private void publishOrderCreatedEvent(Order order) {
        try {
            OrderEvent event = createOrderEvent(order, OrderEvent.ORDER_CREATED);
            eventPublisherService.publishOrderEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish order created event for order: {}", order.getId(), e);
        }
    }

    private void publishOrderUpdatedEvent(Order order, String previousStatus) {
        try {
            OrderEvent event = createOrderEvent(order, OrderEvent.ORDER_UPDATED);
            eventPublisherService.publishOrderEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish order updated event for order: {}", order.getId(), e);
        }
    }

    private void publishOrderCancelledEvent(Order order) {
        try {
            OrderEvent event = createOrderEvent(order, OrderEvent.ORDER_CANCELLED);
            eventPublisherService.publishOrderEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish order cancelled event for order: {}", order.getId(), e);
        }
    }

    private OrderEvent createOrderEvent(Order order, String eventType) {
        List<OrderEvent.OrderItem> eventItems = order.getItems().stream()
                .map(item -> new OrderEvent.OrderItem(
                        item.getProductId(),
                        item.getQuantity(),
                        0.0 // Price will be populated by product service
                ))
                .collect(Collectors.toList());

        return new OrderEvent(
                null, // eventId will be set by publisher
                eventType,
                order.getId(),
                order.getUsername(),
                eventItems,
                0.0, // totalAmount will be calculated
                order.getStatus(),
                LocalDateTime.now()
        );
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(i -> OrderItemDto.builder()
                        .productId(i.getProductId())
                        .quantity(i.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .items(items)
                .status(order.getStatus())
                .username(order.getUsername())
                .build();
    }
}
