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

### Phase 3 ✅
- Product module complete
- Product.java, ProductRepository.java
- ProductMapper.java
- dto/ProductRequest.java, dto/ProductResponse.java
- ProductService.java (@Transactional, tenant-isolated)
- ProductController.java (role based @PreAuthorize)
- common/exception/ResourceNotFoundException.java
- common/exception/GlobalExceptionHandler.java
- V4__create_products.sql
- @EnableJpaAuditing added to EcommerceApplication.java

## Project Structure
