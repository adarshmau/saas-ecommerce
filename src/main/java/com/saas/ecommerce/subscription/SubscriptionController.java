package com.saas.ecommerce.subscription;

import com.saas.ecommerce.subscription.dto.SubscriptionRequest;
import com.saas.ecommerce.subscription.dto.SubscriptionResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<SubscriptionResponse>  getCurrentPlan() {
        return ResponseEntity.ok(
                subscriptionService.getCurrentPlan());
    }
    @PostMapping
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.subscribe(request));
    }
    // PUT upgrade plan
    @PutMapping("/upgrade")
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<SubscriptionResponse> upgrade(
            @Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(
                subscriptionService.upgrade(request));
    }

    // PUT cancel plan
    @PutMapping("/cancel")
    @PreAuthorize("hasRole('STORE_OWNER')")
    public ResponseEntity<SubscriptionResponse> cancel() {
        return ResponseEntity.ok(
                subscriptionService.cancel());
    }
}
