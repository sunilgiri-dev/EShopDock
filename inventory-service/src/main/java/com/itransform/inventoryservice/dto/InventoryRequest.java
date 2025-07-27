package com.itransform.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryRequest {
    @NotBlank
    private String productId;

    @Min(0)
    private int quantity;
}
