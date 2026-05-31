package com.saas.ecommerce.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,String> {

    // Get payment by orderId (one order = one payment)
    Optional<Payment> findByOrderIdAndTenantId(String orderId, String tenantId);

    // Get all payments for a tenant (STORE_OWNER)
    List<Payment> findByTenantId(String tenantId);


    // Get all payments for a customer
    List<Payment> findByCustomerIdAndTenantId(String customerId, String tenantId);

}
