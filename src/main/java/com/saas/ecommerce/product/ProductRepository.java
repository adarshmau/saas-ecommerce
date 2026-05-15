package com.saas.ecommerce.product;


import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,String> {



    List<Product> findByTenantIdAndActiveTrue(String tenantId);

    Optional<Product> findByIdAndTenantId(String id, String tenantId);
}
