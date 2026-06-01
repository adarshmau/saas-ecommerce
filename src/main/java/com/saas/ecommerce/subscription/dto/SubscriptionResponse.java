package com.saas.ecommerce.subscription.dto;

import com.saas.ecommerce.subscription.Plan;
import com.saas.ecommerce.subscription.SubscriptionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubscriptionResponse {
    private String id;
    // ❌ removed tenantId — internal, not needed by client
    private Plan plan;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime cancelledAt;
    private boolean expired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}