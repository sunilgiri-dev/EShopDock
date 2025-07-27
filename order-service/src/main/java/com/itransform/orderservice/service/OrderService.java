package com.itransform.orderservice.service;

import com.itransform.orderservice.dto.*;
import com.itransform.orderservice.exception.ResourceNotFoundException;
import com.itransform.orderservice.model.*;
import com.itransform.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

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
