package com.saas.ecommerce.payment.dto;

import com.saas.ecommerce.payment.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {

    private String id;
    private String orderId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String paymentReference;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
