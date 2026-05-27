package com.saas.ecommerce.order;

import com.saas.ecommerce.order.dto.OrderItemRequest;
import com.saas.ecommerce.order.dto.OrderItemResponse;
import com.saas.ecommerce.order.dto.OrderResponse;
import org.springframework.stereotype.Component;


import java.util.Collections;
import java.util.stream.Collector;
import java.util.stream.Collectors;


@Component
public class OrderMapper {

    // Order Entity → OrderResponse
    public OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .customerEmail(order.getCustomerEmail())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(order.getItems()// get List<OrderItem>
                          .stream()     //convert to stream
                           .map(this::toItemResponse)   //convert each item
                           .collect(Collectors.toList()))  // back to List<OrderItemResponse>
                .build();

    }


    // OrderItem Entity → OrderItemResponse
    private OrderItemResponse toItemResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();

    }









}
