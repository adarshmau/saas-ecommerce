package com.saas.ecommerce.payment;

import com.saas.ecommerce.payment.dto.PaymentRequest;
import com.saas.ecommerce.payment.dto.PaymentResponse;
import com.saas.ecommerce.payment.dto.PaymentVerifyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final RazorpayService razorpayService;

    // Initiate payment → creates Razorpay order
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.initiatePayment(request));
    }

    // Verify payment → validates Razorpay signature
    @PostMapping("/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request) {
        return ResponseEntity.ok(
                paymentService.verifyPayment(request));
    }

    // Get payment status by order
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STORE_OWNER')")
    public ResponseEntity<PaymentResponse> getPaymentByOrder(
            @PathVariable String orderId) {
        return ResponseEntity.ok(
                paymentService.getPaymentByOrder(orderId));
    }
    // ✅ TEST ONLY — remove before production
    @GetMapping("/test-data/{razorpayOrderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Map<String, String>> getTestData(
            @PathVariable String razorpayOrderId) {
        return ResponseEntity.ok(
                razorpayService.generateTestPaymentData(
                        razorpayOrderId));
    }
}