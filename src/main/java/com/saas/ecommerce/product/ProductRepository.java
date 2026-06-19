package com.saas.ecommerce.product;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product,String> {



    List<Product> findByTenantIdAndActiveTrue(String tenantId);


    // Paged listing for products (used for pagination API)
    Page<Product> findByTenantIdAndActiveTrue(String tenantId, Pageable pageable);

    Optional<Product> findByIdAndTenantId(String id, String tenantId);

    // Full-text like search across name, description, category for a tenant
    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.active = true AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.category) LIKE LOWER(CONCAT('%', :q, '%')))" )
    Page<Product> searchByTenantIdAndQuery(@Param("tenantId") String tenantId, @Param("q") String q, Pageable pageable);
}
