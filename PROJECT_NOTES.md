# SaaS Ecommerce - Project Notes

## Tech Stack
- Java 22, Spring Boot 3.5.14
- PostgreSQL 15, Flyway migrations
- JWT (jjwt 0.12.6), Spring Security 6.5
- Lombok, JPA/Hibernate
- Docker (postgres:15, redis:7-alpine)
- Container names: saas_postgres, saas_redis

## Package: com.saas.ecommerce

---

## Completed Phases

### Phase 1 ✅ — Project Setup & Multi-tenancy
- TenantContext.java → ThreadLocal stores tenantId per request
- TenantFilter.java → reads X-Tenant-ID header, sets TenantContext
- DataSourceConfig.java → DB connection config
- V1: tenants table + seed data (tenant-1, tenant-2)
- V2: users table
- V3: fix users (primary key + updated_at)

### Phase 2 ✅ — Auth Module
- User.java → Entity (id, name, email, password, role, tenantId)
- Role.java → Enum (ADMIN, STORE_OWNER, CUSTOMER)
- UserRepository.java → findByEmail, existsByEmail
- AuthService.java → register(name, email, password, tenantId, role), login()
- AuthController.java → POST /auth/register, POST /auth/login
- JwtService.java → generateToken(), isTokenValid(), extractAllClaims()
- JwtFilter.java → reads Bearer token, sets SecurityContext + TenantContext
- SecurityConfig.java → /auth/**, /actuator/** public, rest protected
- @EnableMethodSecurity added to SecurityConfig
- @EnableJpaAuditing added to EcommerceApplication

### Phase 3 ✅ — Product Module
- Product.java → Entity
- ProductRepository.java → findByTenantIdAndActiveTrue, findByIdAndTenantId
- ProductMapper.java → Entity ↔ DTO
- ProductService.java → CRUD, tenant isolated, soft delete
- ProductController.java → REST endpoints
- dto/ProductRequest.java → input with @Valid
- dto/ProductResponse.java → output, no tenantId exposed
- common/exception/ResourceNotFoundException.java
- common/exception/GlobalExceptionHandler.java
- V4: products table

### Phase 4 🔄 — Order Module (in progress)
- OrderStatus.java → Enum (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- Order.java → Entity
- OrderItem.java → Entity (@ToString.Exclude on order field)
- OrderRepository.java → findByTenantId, findByCustomerIdAndTenantId, etc
- OrderItemRepository.java → findByOrderId
- OrderMapper.java → Entity ↔ DTO
- OrderService.java → createOrder, getMyOrders, getAllOrders, updateStatus
- OrderController.java → REST endpoints
- dto/OrderItemRequest.java → productId + quantity
- dto/OrderRequest.java → List<OrderItemRequest>
- dto/OrderResponse.java → clean response
- dto/OrderItemResponse.java → item response
- V5: orders + order_items tables

### Phase 5 🔜 — Payment Module
- Payment.java → Entity
- PaymentStatus.java → Enum (PENDING, COMPLETED, FAILED, REFUNDED)
- PaymentRepository.java
- PaymentMapper.java
- PaymentService.java → initiate, verify, refund
- PaymentController.java
- dto/PaymentRequest.java
- dto/PaymentResponse.java
- V6: payments table

### Phase 6 🔜 — Subscription Module
- Plan.java → Enum (FREE, BASIC, PREMIUM)
- Subscription.java → Entity (tenantId, plan, startDate, endDate, status)
- SubscriptionRepository.java
- SubscriptionMapper.java
- SubscriptionService.java → subscribe, upgrade, cancel, checkExpiry
- SubscriptionController.java
- dto/SubscriptionRequest.java
- dto/SubscriptionResponse.java
- V7: subscriptions table

### Phase 7 🔜 — Notification Module
- NotificationType.java → Enum (ORDER_PLACED, ORDER_CONFIRMED, ORDER_SHIPPED, ORDER_DELIVERED)
- Notification.java → Entity
- NotificationRepository.java
- NotificationService.java → send on order status change
- NotificationController.java → get my notifications
- V8: notifications table

### Phase 8 🔜 — Analytics Module
- AnalyticsService.java → revenue, orders count, top products
- AnalyticsController.java
- Endpoints for STORE_OWNER dashboard

### Phase 9 🔜 — API Improvements
- Pagination for products and orders
- Search and filter products
- Rate limiting
- API versioning (/api/v1/)
- Swagger/OpenAPI documentation

---

## Full Project Structure

com.saas.ecommerce/
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── JwtService.java
│   ├── User.java
│   ├── Role.java
│   └── UserRepository.java
├── config/
│   ├── SecurityConfig.java
│   ├── JwtFilter.java
│   └── DataSourceConfig.java
├── tenant/
│   ├── TenantContext.java
│   └── TenantFilter.java
├── product/
│   ├── Product.java
│   ├── ProductRepository.java
│   ├── ProductMapper.java
│   ├── ProductService.java
│   ├── ProductController.java
│   └── dto/
│       ├── ProductRequest.java
│       └── ProductResponse.java
├── order/
│   ├── Order.java
│   ├── OrderItem.java
│   ├── OrderStatus.java
│   ├── OrderRepository.java
│   ├── OrderItemRepository.java
│   ├── OrderMapper.java
│   ├── OrderService.java
│   ├── OrderController.java
│   └── dto/
│       ├── OrderRequest.java
│       ├── OrderItemRequest.java
│       ├── OrderResponse.java
│       └── OrderItemResponse.java
├── payment/
│   ├── Payment.java
│   ├── PaymentStatus.java
│   ├── PaymentRepository.java
│   ├── PaymentMapper.java
│   ├── PaymentService.java
│   ├── PaymentController.java
│   └── dto/
│       ├── PaymentRequest.java
│       └── PaymentResponse.java
├── subscription/
│   ├── Plan.java
│   ├── Subscription.java
│   ├── SubscriptionRepository.java
│   ├── SubscriptionMapper.java
│   ├── SubscriptionService.java
│   ├── SubscriptionController.java
│   └── dto/
│       ├── SubscriptionRequest.java
│       └── SubscriptionResponse.java
├── notification/
│   ├── Notification.java
│   ├── NotificationType.java
│   ├── NotificationRepository.java
│   ├── NotificationService.java
│   ├── NotificationController.java
│   └── dto/
│       └── NotificationResponse.java
├── analytics/
│   ├── AnalyticsService.java
│   ├── AnalyticsController.java
│   └── dto/
│       └── AnalyticsResponse.java
└── common/
└── exception/
├── ResourceNotFoundException.java
└── GlobalExceptionHandler.java


---

## DB Migrations
- V1: tenants table + 2 seed tenants (tenant-1, tenant-2)
- V2: users table
- V3: added primary key + updated_at to users
- V4: products table
- V5: orders + order_items tables
- V6: payments table (Phase 5)
- V7: subscriptions table (Phase 6)
- V8: notifications table (Phase 7)

---

## API Endpoints

### Auth (public)
- POST /auth/register → register user
- POST /auth/login    → login, returns JWT token

### Products (protected)
- GET    /api/products       → all active products
- GET    /api/products/{id}  → single product
- POST   /api/products       → create (STORE_OWNER)
- PUT    /api/products/{id}  → update (STORE_OWNER)
- DELETE /api/products/{id}  → soft delete (STORE_OWNER)

### Orders (protected)
- POST  /api/orders              → create order (CUSTOMER)
- GET   /api/orders/my-orders    → my orders (CUSTOMER)
- GET   /api/orders/{id}         → single order (CUSTOMER + STORE_OWNER)
- GET   /api/orders              → all orders (STORE_OWNER)
- PATCH /api/orders/{id}/status  → update status (STORE_OWNER)

### Payments (Phase 5)
- POST /api/payments             → initiate payment (CUSTOMER)
- GET  /api/payments/{orderId}   → payment status (CUSTOMER + STORE_OWNER)

### Subscriptions (Phase 6)
- GET  /api/subscriptions        → current plan (STORE_OWNER)
- POST /api/subscriptions        → subscribe to plan (STORE_OWNER)
- PUT  /api/subscriptions/upgrade → upgrade plan (STORE_OWNER)
- PUT  /api/subscriptions/cancel  → cancel plan (STORE_OWNER)

### Notifications (Phase 7)
- GET /api/notifications         → my notifications (authenticated)
- PUT /api/notifications/{id}/read → mark as read

### Analytics (Phase 8)
- GET /api/analytics/revenue     → revenue stats (STORE_OWNER)
- GET /api/analytics/orders      → order stats (STORE_OWNER)
- GET /api/analytics/products    → top products (STORE_OWNER)

---

## Order Status Flow-------
----------------------------------------------------------------------
PENDING → CONFIRMED → SHIPPED → DELIVERED
↘ CANCELLED  ↗

## Payment Status Flow
PENDING → COMPLETED
↘ FAILED
COMPLETED → REFUNDED
---------------------------------------------------

---

## Key Architecture Decisions
- Layered architecture (Controller → Service → Repository)
- Soft delete for products (active = false)
- Price snapshot in order_items (locked at purchase time)
- Stock deduction on order creation
- customerId/email from JWT (never from request body)
- tenantId from TenantContext (never from request body)
- @Transactional on all write operations
- BigDecimal for all money fields (never double/float)
- Status transition validation in OrderService
- Separate Request/Response DTOs per module
- Mapper classes for entity ↔ DTO conversion
- GlobalExceptionHandler for consistent error responses

---

## Test Data
- Tenant IDs: tenant-1, tenant-2
- CUSTOMER: 
- STORE_OWNER: 

## Postman Headers

