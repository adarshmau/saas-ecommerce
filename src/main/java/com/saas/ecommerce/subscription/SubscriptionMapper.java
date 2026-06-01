package com.saas.ecommerce.subscription;

import com.saas.ecommerce.subscription.dto.SubscriptionResponse;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {


    public SubscriptionResponse toResponse(Subscription  subscription) {

        SubscriptionResponse subscriptionResponse = new SubscriptionResponse();
        subscriptionResponse.setId(subscription.getId());
        subscriptionResponse.setPlan(subscription.getPlan());
        subscriptionResponse.setStatus(subscription.getStatus());
        subscriptionResponse.setStartDate(subscription.getStartDate());
        subscriptionResponse.setEndDate(subscription.getEndDate());
        subscriptionResponse.setCancelledAt(subscription.getCancelledAt());
        subscriptionResponse.setCreatedAt(subscription.getCreatedAt());
        subscriptionResponse.setUpdatedAt(subscription.getUpdatedAt());


        // ✅ Computed field — never stored in DB
        // expired = true if endDate exists AND endDate is in the past
        subscriptionResponse.setExpired(
                subscription.getEndDate() != null &&
                        subscription.getEndDate().isBefore(subscription.getStartDate())
        );
        return subscriptionResponse;
    }



}
