package com.saas.ecommerce.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.saas.ecommerce.order.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {

    private String id;
    private String customerEmail;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;

    @JsonFormat(pattern = "yyyy-MM-dd HH:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:ss")
    private LocalDateTime updatedAt;

}

//*{
//    "id": "order-123",
//    "customerEmail": "adarsh@test.com",
//    "status": "PENDING",
//    "totalAmount": 1998.00,
//    "createdAt": "2026-05-15 10:00:00",
//    "items": [
//        {
//            "id": "item-1",
//            "productId": "abc-123",
//            "productName": "iPhone 15",
//            "quantity": 2,
//            "unitPrice": 999.00,
//            "totalPrice": 1998.00
//        }
//    ]
//}*/
