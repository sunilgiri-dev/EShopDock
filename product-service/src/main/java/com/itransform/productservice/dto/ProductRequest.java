package com.itransform.productservice.dto;

import jakarta.validation.constraints.*;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {
    @NotBlank(message = "Product name cannot be blank")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Description cannot be blank")
    @Size(max = 255)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private BigDecimal price;
}
