package com.learn.orderservice.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private String id;
    private List<OrderItemDto> items;
    private String status;
    private String username;
}
