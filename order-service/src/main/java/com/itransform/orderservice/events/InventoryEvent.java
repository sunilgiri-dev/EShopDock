package com.itransform.orderservice.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryEvent {
    private String eventId;
    private String eventType;
    private String inventoryId;
    private String productId;
    private Integer quantity;
    private Integer previousQuantity;
    private LocalDateTime timestamp;

    // Event types
    public static final String INVENTORY_UPDATED = "INVENTORY_UPDATED";
    public static final String INVENTORY_LOW_STOCK = "INVENTORY_LOW_STOCK";
    public static final String INVENTORY_OUT_OF_STOCK = "INVENTORY_OUT_OF_STOCK";
    public static final String INVENTORY_RESTOCKED = "INVENTORY_RESTOCKED";
}