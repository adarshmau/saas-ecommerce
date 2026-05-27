package com.saas.ecommerce.order;

import org.springframework.data.jpa.repository.JpaRepository;

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



}
