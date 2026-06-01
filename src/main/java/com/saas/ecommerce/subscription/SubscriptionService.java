package com.saas.ecommerce.subscription;


import com.saas.ecommerce.common.exception.ResourceNotFoundException;
import com.saas.ecommerce.subscription.dto.SubscriptionRequest;
import com.saas.ecommerce.subscription.dto.SubscriptionResponse;
import com.saas.ecommerce.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;

    //Get current plan------------------------------------------------------------------
    @Transactional
    public SubscriptionResponse getCurrentPlan() {
        String tenantId = TenantContext.getTenantId();
        return subscriptionRepository.findByTenantId(tenantId)
                .map(subscriptionMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Subscription not found for this tenant Id: " + tenantId));

    }

    // SUBSCRIBE---------------------------------------------------------------------------
    @Transactional
    public SubscriptionResponse subscribe(SubscriptionRequest request) {
        String tenantId = TenantContext.getTenantId();

        Optional<Subscription> existing = subscriptionRepository.findByTenantId(tenantId);

        //  If cancelled subscription exists — reactivate it
        if (existing.isPresent()) {
            Subscription s = existing.get();

            if (s.getStatus() != SubscriptionStatus.CANCELLED) {
                throw new IllegalStateException(
                        "Already has an active subscription. Use upgrade instead.");
            }
            // Reactivate cancelled subscription
            s.setPlan(request.getPlan());
            s.setStatus(SubscriptionStatus.ACTIVE);
            s.setStartDate(LocalDateTime.now());
            s.setEndDate(calculateEndDate(request.getPlan()));
            s.setCancelledAt(null);

            Subscription saved = subscriptionRepository.save(s);
            log.info("Tenant {} resuscribed to plan:{}", tenantId, s.getPlan());
            return subscriptionMapper.toResponse(saved);
        }
        // Fresh subscription
        Subscription subscription = new Subscription();
        subscription.setTenantId(tenantId);
        subscription.setPlan(request.getPlan());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(calculateEndDate(request.getPlan()));

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Tenant {} subscribed to plan: {}",
                tenantId, request.getPlan());
        return subscriptionMapper.toResponse(saved);
    }

    //─── PRIVATE HELPERS------------------------------------------------------
    private LocalDateTime calculateEndDate(Plan plan) {
        return switch (plan) {
            case FREE -> null;
            case BASIC, PREMIUM -> LocalDateTime.now().plusMonths(1);
        };
    }

    // ─── UPGRADE------------------------------------------------------------------
    public SubscriptionResponse upgrade(SubscriptionRequest request) {
        String tenantId = TenantContext.getTenantId();

        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No subscription found for tenant: " + tenantId));

        //Block CANCELLED and EXPIRED
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED ||
                subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            log.warn("Upgrade attempt on {} subscription for tenant: {}",
                    subscription.getStatus(), tenantId);
            throw new IllegalStateException(
                    "Cannot upgrade a " + subscription.getStatus()
                            + " subscription. Please subscribe again.");


        }
        // Same plan check
        if (subscription.getPlan() == request.getPlan()) {
            throw new IllegalStateException("You are already on the " +
                    request.getPlan() + "plan");
        }

        // Downgrade check
        if (!isUpgrade(subscription.getPlan(), request.getPlan())) {
            log.warn("Downgrade attempt from {} to {} for tenant: {}",
                    subscription.getPlan(), request.getPlan(), tenantId);
            throw new IllegalStateException(
                    "Can only upgrade to a higher plan. Current: "
                            + subscription.getPlan()
                            + ", Requested: " + request.getPlan());
        }
        subscription.setPlan(request.getPlan());
        subscription.setStartDate(LocalDateTime.now());
        subscription.setEndDate(calculateEndDate(request.getPlan()));
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Tenant {} upgraded to plan: {}",
                tenantId, request.getPlan());
        return subscriptionMapper.toResponse(saved);

    }

    //private Halper method----------------------------------
    private boolean isUpgrade(Plan current, Plan requested) {
        return planLevel(requested) > planLevel(current);
    }

    private int planLevel(Plan plan) {
        return switch (plan) {
            case FREE -> 1;
            case BASIC -> 2;
            case PREMIUM -> 3;
        };
    }

   // --------------------------------------canceled ------------------
   @Transactional
   public SubscriptionResponse cancel() {
       String tenantId = TenantContext.getTenantId();

       Subscription subscription = subscriptionRepository
               .findByTenantId(tenantId)
               .orElseThrow(() -> new ResourceNotFoundException(
                       "No subscription found for tenant: " + tenantId));

       if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
           throw new IllegalStateException(
                   "Subscription is already cancelled");
       }

       subscription.setStatus(SubscriptionStatus.CANCELLED);
       subscription.setCancelledAt(LocalDateTime.now()); // ✅ track when cancelled

       Subscription saved = subscriptionRepository.save(subscription);
       log.info("Tenant {} cancelled subscription", tenantId);
       return subscriptionMapper.toResponse(saved);
   }
}
