package com.saas.ecommerce.analytics.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopProductResponse {
    private String productId;
    private String productName;
    private Long totalQuantitySold;
    private BigDecimal totalRevenue;
}
