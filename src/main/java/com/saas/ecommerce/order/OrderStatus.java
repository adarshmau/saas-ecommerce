package com.saas.ecommerce.order;

public enum OrderStatus {
    PENDING,              // Order placed, awaiting confirmation
    CONFIRMED, // Store owner confirmed
    SHIPPED,
    DELIVERED,
    CANCELLED

}
