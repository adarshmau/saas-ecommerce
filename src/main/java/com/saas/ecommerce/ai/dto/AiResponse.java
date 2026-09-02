package com.saas.ecommerce.ai.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiResponse {

    private String productName;
    private String description;
    private boolean aiGenerated;
}
