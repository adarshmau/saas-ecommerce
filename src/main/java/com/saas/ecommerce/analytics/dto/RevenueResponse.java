package com.saas.ecommerce.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class RevenueResponse {

    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private BigDecimal thisMonthRevenue;
    private Long totalOrders;
    private Long completedOrders;
}
