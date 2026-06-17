package com.saas.ecommerce.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


public interface OrderRepository extends JpaRepository<Order,String> {

    //1 Get all orders for a tenant
       List<Order> findByTenantId(String tenantId);

    // 2 Get all orders for a specific customer in a tenant
    List<Order> findByCustomerIdAndTenantId(String customerId,String tenantId);


    // 3 Get single order by id and tenant (security check)
    Optional<Order> findByIdAndTenantId(String id, String tenantId);//Same pattern as products — always verify tenant!


    //  4 Get orders by status for a tenant (STORE_OWNER use case)
    List<Order> findByTenantIdAndStatus(String TenantId,String Status);//"show me all PENDING orders"


    // Count orders by status for a tenant
    Long countByTenantIdAndStatus(String tenantId, OrderStatus status);

    // Count all orders for a tenant
    Long countByTenantId(String tenantId);

    @Query("SELECT COALESCE(SUM(o.totalAmount),0)" +
            "FROM Order o where o.tenantId=:tenantId " +
            "And o.status='DELIVERED'")
    BigDecimal findTotalRevenueByTenantId(@Param("tenantId") String tenantId);

    @Query("Select COALESCE(SUM(o.totalAmount),0)"+
            "from Order o where o.tenantId=:tenantId " +
            "And o.status='DELIVERED' " +
            "AND CAST(o.createdAt as date )= CURRENT_DATE")
    BigDecimal findTodayRevenueByTenantId(@Param("tenantId") String tenantId);


    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) " +
            "FROM Order o WHERE o.tenantId = :tenantId " +
            "AND o.status = 'DELIVERED' " +
            "AND MONTH(o.createdAt) = MONTH(CURRENT_DATE) " +
            "AND YEAR(o.createdAt) = YEAR(CURRENT_DATE)")
    BigDecimal findThisMonthRevenueByTenantId(@Param("tenantId") String tenantId);








}
