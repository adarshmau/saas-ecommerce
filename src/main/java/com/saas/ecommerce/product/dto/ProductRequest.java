
package com.saas.ecommerce.product.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {

        @NotBlank(message = "Product name is required")
        @Size(max = 200, message = "Name must be under 200 characters")
        private String name;

        private String description;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        private BigDecimal price;

        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock cannot be negative")
        private Integer stockQuantity;

        @Size(max = 100, message = "Category must be under 100 characters")
        private String category;

        private String imageUrl;
    }

