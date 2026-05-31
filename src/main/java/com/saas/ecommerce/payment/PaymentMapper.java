package com.saas.ecommerce.payment;

import com.saas.ecommerce.payment.dto.PaymentResponse;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment  payment) {

        PaymentResponse paymentResponse = new PaymentResponse();

        paymentResponse.setId(payment.getId());
        paymentResponse.setOrderId((payment.getOrderId()));
        paymentResponse.setAmount(payment.getAmount());
        paymentResponse.setStatus(payment.getStatus());
        paymentResponse.setPaymentReference(payment.getPaymentReference());
        paymentResponse.setFailureReason(payment.getFailureReason());
        paymentResponse.setCreatedAt(payment.getCreatedAt());
        paymentResponse.setUpdatedAt(payment.getUpdatedAt());

        return paymentResponse;
    }
}
