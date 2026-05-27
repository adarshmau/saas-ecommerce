package com.saas.ecommerce.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemRequest {

    @NotBlank(message = "Product id is required")
    private String ProductId;

    @NotNull(message = "Quantity is required")
    @Min(value=1,message = "Quantity must be at least 1")
    private Integer Quantity;
}

//    "productId": "abc-123",
//    "quantity": 2
