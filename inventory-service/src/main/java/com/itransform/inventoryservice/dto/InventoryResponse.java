package com.itransform.inventoryservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {
    private String id;
    private String productId;
    private int quantity;
}
