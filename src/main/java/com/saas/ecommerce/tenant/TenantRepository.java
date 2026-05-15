package com.saas.ecommerce.tenant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant,String> {

    Optional<Tenant> findBySubdomain(String subdomain);

    Optional<Tenant> findByEmail(String email);





}
