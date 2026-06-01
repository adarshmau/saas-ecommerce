package com.saas.ecommerce.subscription.dto;

import com.saas.ecommerce.subscription.Plan;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionRequest {

    @NotNull(message = "Plan is required. Valid values: FREE, BASIC, PREMIUM")
    private Plan plan;


}
