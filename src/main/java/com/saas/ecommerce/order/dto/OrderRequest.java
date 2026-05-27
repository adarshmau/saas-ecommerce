package com.saas.ecommerce.order.dto;


import com.saas.ecommerce.order.OrderItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {

    @NotEmpty(message = "order must have at least one item")
    @Valid    //each item must have valid productId and quantity
    private List<OrderItemRequest> items;
}

//{
  //  "items": [
    //{ "productId": "abc-123", "quantity": 2 },
    //{ "productId": "xyz-456", "quantity": 1 }
    //]
//}