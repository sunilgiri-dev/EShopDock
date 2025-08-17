package com.itransform.orderservice.events;

import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductEvent {
    private String eventId;
    private String eventType;
    private String productId;
    private String name;
    private String description;
    private Double price;
    private LocalDateTime timestamp;

    // Event types
    public static final String PRODUCT_CREATED = "PRODUCT_CREATED";
    public static final String PRODUCT_UPDATED = "PRODUCT_UPDATED";
    public static final String PRODUCT_DELETED = "PRODUCT_DELETED";


}