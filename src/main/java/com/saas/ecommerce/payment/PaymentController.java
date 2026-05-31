package com.saas.ecommerce.payment;

import com.saas.ecommerce.payment.dto.PaymentRequest;
import com.saas.ecommerce.payment.dto.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody PaymentRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                             .body(paymentService.initiatePayment(request));
    }
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> completePayment(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.completePayment(id));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER','STORE_OWNER')")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable String OrderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrder(OrderId));
    }

    @PatchMapping("/{id}/fail")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> failPayment(
            @PathVariable String id,
            @RequestParam(defaultValue = "Payment failed") String reason) {
        return ResponseEntity.ok(paymentService.failPayment(id, reason));
    }

}
