package com.saas.ecommerce.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name too long")
    private String productName;

    private String category;
    private String targetAudience;
}
