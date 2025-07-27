package com.itransform.orderservice.controller;

import com.itransform.orderservice.dto.*;
import com.itransform.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest orderRequest,
            @RequestHeader("username") String username // Set by Gateway, or extracted from JWT in a real setup
    ) {
        OrderResponse created = orderService.createOrder(orderRequest, username);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders(
            @RequestHeader("role") String role,
            @RequestHeader("username") String username
    ) {
        if ("USER".equals(role)) {
            // Only see own orders
            return ResponseEntity.ok(orderService.getOrdersForUser(username));
        }
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }
}
