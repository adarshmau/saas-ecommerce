package com.saas.ecommerce.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    // One subscription per tenant
    Optional<Subscription> findByTenantId(String tenantId);
}
