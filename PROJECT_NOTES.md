# SaaS Ecommerce - Project Notes

## Tech Stack
- Java 22, Spring Boot 3.5.14
- PostgreSQL 15, Flyway migrations
- JWT (jjwt 0.12.6), Spring Security 6.5
- Lombok, JPA/Hibernate

## Package: com.saas.ecommerce

## Completed Phases

### Phase 1 ✅
- Project setup
- Multi-tenancy: TenantContext.java (ThreadLocal), TenantFilter.java
- DataSourceConfig.java
- DB migrations V1 (tenants), V2 (users), V3 (fix users)

### Phase 2 ✅
- Auth module: User, Role, UserRepository
- AuthService, AuthController
- JwtService (generateToken, validateToken, extractClaims)
- JwtFilter (reads Bearer token, sets SecurityContext, sets TenantContext)
- SecurityConfig (/auth/** and /actuator/** are public, rest need token)
- application.yml configured

## DB Migrations
- V1: tenants table + 2 seed tenants (tenant-1, tenant-2)
- V2: users table
- V3: added primary key + updated_at to users

## Test Data
- Tenant IDs: tenant-1, tenant-2
- Test user: adarsh@test.com / 123456

## Current Phase
### Phase 3 🔜 — Product Module
- Product.java entity
- ProductRepository.java
- ProductService.java (tenant-isolated CRUD)
- ProductController.java
- ProductDTO.java
- V4__create_products.sql
- STORE_OWNER can create/edit/delete
- CUSTOMER can only view
- 
  {
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZGFyc2hAdGVzdC5jb20iLCJyb2xlIjoiQ1VTVE9NRVIiLCJ0ZW5hbnRJZCI6InRlbmFudC0xIiwiaWF0IjoxNzc4MjM3MzM1LCJleHAiOjE3NzgzMjM3MzV9.LPN-htSaBLKrosvxsiNtmzet-4wpTxqU9Nz5ARMGcogyiVReztEzAMpIB1Xuj9uWaO4VolZ0Jfv5U52mAfFIbA"
  }