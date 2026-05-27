package com.saas.ecommerce.order;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface  OrderItemRepository extends JpaRepository<OrderItem,String> {

    // Get all items for a specific order
    List <OrderItem> findByOrderId(String  OrderId);



}
