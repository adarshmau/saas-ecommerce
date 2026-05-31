package com.saas.ecommerce.payment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotBlank(message = "Order id is requried")
    private String orderId;
}
