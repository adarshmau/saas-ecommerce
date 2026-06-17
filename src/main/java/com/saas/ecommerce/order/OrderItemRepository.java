package com.saas.ecommerce.order;

import com.saas.ecommerce.analytics.dto.TopProductResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface  OrderItemRepository extends JpaRepository<OrderItem,String> {

    // Get all items for a specific order
    List <OrderItem> findByOrderId(String  OrderId);

    // Top _selling products
    @Query("SELECT new com.saas.ecommerce.analytics.dto.TopProductResponse(" +
            "oi.productId, oi.productName, " +
            "SUM(oi.quantity), SUM(oi.totalPrice)) " +
            "FROM OrderItem oi " +
            "WHERE oi.order.tenantId = :tenantId " +
            "GROUP BY oi.productId, oi.productName " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<TopProductResponse> findTopProductsByTenantId(
            @Param("tenantId") String tenantId,
            Pageable pageable);



}
