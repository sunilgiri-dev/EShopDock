package com.learn.orderservice.controller;


import com.learn.orderservice.dto.OrderRequest;
import com.learn.orderservice.dto.OrderResponse;
import com.learn.orderservice.service.OrderService;
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

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String id,
            @RequestParam String status,
            @RequestHeader("role") String role
    ) {
        // Only ADMIN, MANAGER, and SUPPORT can update order status
        if (!"ADMIN".equals(role) && !"MANAGER".equals(role) && !"SUPPORT".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        OrderResponse response = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String id,
            @RequestHeader("role") String role,
            @RequestHeader("username") String username
    ) {
        // Users can only cancel their own orders, others can cancel any order
        OrderResponse response;
        if ("USER".equals(role)) {
            // Additional validation would be needed here to ensure the user owns the order
            response = orderService.cancelOrder(id);
        } else if ("ADMIN".equals(role) || "MANAGER".equals(role) || "SUPPORT".equals(role)) {
            response = orderService.cancelOrder(id);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(response);
    }
}
