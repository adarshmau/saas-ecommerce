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

- ## DB Migrations
- V1: tenants table + 2 seed tenants (tenant-1, tenant-2)
- V2: users table
- V3: added primary key + updated_at to users
- V4: products table
- V5: orders + order_items tables (Phase 4)

## Test Data
- Tenant IDs: tenant-1, tenant-2
- Test user (CUSTOMER): adarsh@test.com /password
- Test user (STORE_OWNER): owner2@test.com /password

## API Endpoints

### Auth (public)
- POST /auth/register → register user
- POST /auth/login → login, returns JWT token

### Products (protected)
- GET    /api/products      → get all products
- GET    /api/products/{id} → get single product
- POST   /api/products      → create product (STORE_OWNER)
- PUT    /api/products/{id} → update product (STORE_OWNER)
- DELETE /api/products/{id} → soft delete (STORE_OWNER)

### Orders (Phase 4 - coming)
- POST   /api/orders        → create order (CUSTOMER)
- GET    /api/orders        → get my orders (CUSTOMER)
- GET    /api/orders/{id}   → get single order
- PATCH  /api/orders/{id}/status → update status (STORE_OWNER)

## Postman Headers
- Content-Type: application/json
- Authorization: Bearer <token>
- X-Tenant-ID: tenant-1

## Order Status Flow
PENDING → CONFIRMED → SHIPPED → DELIVERED

## Current Phase
### Phase 4 🔜 — Order Module
- V5__create_orders.sql
- Order.java entity
- OrderItem.java entity
- OrderRepository.java
- OrderItemRepository.java
- OrderMapper.java
- OrderService.java
- OrderController.java
- dto/OrderRequest.java
- dto/OrderItemRequest.java
- dto/OrderResponse.java

## Notes
- Soft delete used for products (active = false)
- All data is tenant isolated via TenantContext (ThreadLocal)
- JWT contains: email, role, tenantId
- Flyway manages all DB migrations
- ddl-auto: none (Flyway handles schema)
- @EnableJpaAuditing for auto timestamps
- @EnableMethodSecurity for @PreAuthorize

## Project Structure
