# ShopSync AI — Project Notes
## AI-Powered Multi-Tenant SaaS Ecommerce Platform

---

## Tech Stack
- Java 22, Spring Boot 3.5.14
- PostgreSQL 15, Flyway migrations
- JWT (jjwt 0.12.6), Spring Security 6.5
- Lombok, JPA/Hibernate
- Docker (postgres:15, redis:7-alpine)
- Container names: saas_postgres, saas_redis
- Gmail SMTP (spring-boot-starter-mail)
- Redis (spring-boot-starter-data-redis)
- OpenAI API (Phase 10)

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
- AuthService.java → register(), login()
- AuthController.java → POST /auth/register, POST /auth/login
- JwtService.java → generateToken(), isTokenValid(), extractAllClaims(), getExpirySeconds()
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

### Phase 4 ✅ — Order Module
- OrderStatus.java → Enum (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- Order.java → Entity (@Getter @Setter @NoArgsConstructor)
- OrderItem.java → Entity (@ToString.Exclude on order field)
- OrderRepository.java → findByTenantId, findByCustomerIdAndTenantId,
  findByIdAndTenantId (Optional), findByTenantIdAndStatus,
  countByTenantId, countByTenantIdAndStatus,
  findTotalRevenueByTenantId, findTodayRevenueByTenantId,
  findThisMonthRevenueByTenantId
- OrderItemRepository.java → findByOrderId, findTopProductsByTenantId
- OrderMapper.java → Entity ↔ DTO
- OrderService.java → createOrder, getMyOrders, getOrder, getAllOrders, updateStatus
- OrderController.java → REST endpoints with role-based access
- dto/OrderItemRequest.java → productId + quantity
- dto/OrderRequest.java → List<OrderItemRequest>
- dto/OrderResponse.java → clean response
- dto/OrderItemResponse.java → item response
- common/exception/InvalidStatusTransitionException.java
- V5: orders + order_items tables

#### Key Order Decisions
- Price snapshot in order_items (locked at purchase time)
- Stock deduction on order creation inside @Transactional
- customerId from JWT only — never from request body
- tenantId from TenantContext — never from request body
- @Transactional(readOnly = true) on all read methods
- Ownership check in getOrder — CUSTOMER can only see their own orders
- Status transitions: PENDING → CONFIRMED → SHIPPED → DELIVERED
- PENDING or CONFIRMED → CANCELLED allowed
- DELIVERED and CANCELLED are terminal states (locked)

### Phase 5 ✅ — Payment Module
- PaymentStatus.java → Enum (PENDING, COMPLETED, FAILED, REFUNDED)
- Payment.java → Entity (@Getter @Setter @NoArgsConstructor)
- PaymentRepository.java → findByOrderIdAndTenantId (Optional),
  findByTenantId, findByCustomerIdAndTenantId
- PaymentMapper.java → Entity ↔ DTO
- PaymentService.java → initiatePayment, completePayment, failPayment,
  getPaymentByOrder
- PaymentController.java → REST endpoints
- dto/PaymentRequest.java → orderId with @NotBlank
- dto/PaymentResponse.java → clean response
- V6: payments table

#### Key Payment Decisions
- One payment per order — unique constraint on order_id at DB + application level
- customerId from JWT only — never from request body
- tenantId verified on every query
- Only PENDING orders can be paid
- completePayment → Payment COMPLETED + Order auto CONFIRMED (atomic)
- failPayment → Payment FAILED + Stock restored + Order CANCELLED (atomic)
- Payment reference simulated (PAY-XXXXXXXX) → ready for Razorpay later
- sendPaymentNotification() called after completePayment and failPayment

### Phase 6 ✅ — Subscription Module
- Plan.java → Enum (FREE, BASIC, PREMIUM)
- SubscriptionStatus.java → Enum (ACTIVE, CANCELLED, EXPIRED)
- Subscription.java → Entity (tenantId unique, cancelledAt, updatedAt)
- SubscriptionRepository.java → findByTenantId (Optional)
- SubscriptionMapper.java → toResponse() with computed expired field
- SubscriptionService.java → getCurrentPlan, subscribe, upgrade, cancel
- SubscriptionController.java → REST endpoints (STORE_OWNER only)
- dto/SubscriptionRequest.java → plan with @NotNull
- dto/SubscriptionResponse.java → expired computed field, no tenantId
- V7: subscriptions table

#### Key Subscription Decisions
- One subscription per tenant — unique constraint on tenant_id
- FREE plan → null end date (never expires)
- BASIC / PREMIUM → end date = now + 1 month
- Upgrade only (no downgrade) — planLevel() validates direction
- Cannot upgrade CANCELLED or EXPIRED subscription
- Resubscribe allowed after cancellation (reactivates existing record)
- cancelledAt tracked when subscription cancelled
- expired field computed at runtime from endDate — never stored in DB
- Plan levels: FREE=1, BASIC=2, PREMIUM=3

### Phase 7 ✅ — Notification Module
- NotificationType.java → Enum (ORDER_PLACED, ORDER_CONFIRMED,
  ORDER_SHIPPED, ORDER_DELIVERED, ORDER_CANCELLED,
  PAYMENT_COMPLETED, PAYMENT_FAILED)
- Notification.java → Entity (Boolean read, readAt, updatedAt)
- NotificationRepository.java → findByUserIdAndTenantIdOrderByCreatedAtDesc,
  findByUserIdAndTenantIdAndReadFalse, findByIdAndTenantId
- NotificationMapper.java → maps readAt, updatedAt
- NotificationService.java → sendOrderNotification, sendPaymentNotification,
  getMyNotifications, markAsRead
- NotificationController.java → GET /api/notifications,
  PUT /api/notifications/{id}/read
- dto/NotificationResponse.java → id, orderId, type, message,
  read, readAt, createdAt, updatedAt
- V8: notifications table (is_read, read_at, updated_at columns)
- Real email via Gmail SMTP (spring-boot-starter-mail)

#### Key Notification Decisions
- Triggered automatically from OrderService and PaymentService
- @Transactional(propagation = Propagation.REQUIRED) — joins existing tx
- Saved to DB + real email sent via Gmail SMTP
- Email failure NEVER breaks order/payment flow (try/catch isolation)
- User can only mark their own notifications as read
- Ordered by createdAt DESC — newest first
- Boolean (wrapper) not boolean (primitive) for JPA compatibility
- readAt set when markAsRead() called

### Phase 8 ✅ — Analytics Module
- AnalyticsService.java → getRevenue, getOrderStats, getTopProducts
- AnalyticsController.java → STORE_OWNER only endpoints
- dto/RevenueResponse.java → totalRevenue, todayRevenue,
  thisMonthRevenue, totalOrders, completedOrders
- dto/OrderStatsResponse.java → breakdown by all statuses
- dto/TopProductResponse.java → productId, productName,
  totalQuantitySold, totalRevenue
- Analytics queries added to OrderRepository (existing file)
- Top products query added to OrderItemRepository (existing file)

#### Key Analytics Decisions
- No new migration needed — queries existing tables
- COALESCE used for null-safe revenue calculations
- FUNCTION() used for MONTH/YEAR (JPQL portable syntax)
- Full enum path used in JPQL status comparisons
- Top 10 products using Pageable (PageRequest.of(0, 10))
- @Transactional(readOnly = true) on all analytics methods
- OrderRepository NOT duplicated — analytics methods added to
  existing order/OrderRepository.java

### Phase 9 🔜 — API Improvements + Redis
- Swagger/OpenAPI documentation (springdoc-openapi)
- Pagination for products and orders
- Rate limiting
- API versioning (/api/v1/)
- Redis JWT blacklisting for secure logout
- TokenBlacklistService.java
- Logout endpoint → POST /auth/logout

### Phase 10 🔜 — AI Features (OpenAI)
- AiService.java → generateProductDescription()
- AI analytics summary
- OpenAI API integration

### Phase 11 🔜 — Production Enhancements
- Razorpay payment gateway
- Kafka for async notifications
- Unit tests (JUnit + Mockito)
- Integration tests

### Phase 12 🔜 — React Frontend
- React 18 + Vite
- Tailwind CSS + shadcn/ui
- Framer Motion animations
- Axios + React Query
- Customer and Store Owner dashboards

---

## Full Project Structure

```
com.saas.ecommerce/
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── JwtService.java
│   ├── User.java
│   ├── Role.java
│   ├── UserRepository.java
│   └── TokenBlacklistService.java       (Phase 9)
├── config/
│   ├── SecurityConfig.java
│   ├── JwtFilter.java
│   ├── DataSourceConfig.java
│   └── RedisConfig.java                 (Phase 9)
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
│   ├── OrderRepository.java             (includes analytics queries)
│   ├── OrderItemRepository.java         (includes top products query)
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
│   ├── SubscriptionStatus.java
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
│   ├── NotificationMapper.java
│   ├── NotificationService.java
│   ├── NotificationController.java
│   └── dto/
│       └── NotificationResponse.java
├── analytics/
│   ├── AnalyticsService.java
│   ├── AnalyticsController.java
│   └── dto/
│       ├── RevenueResponse.java
│       ├── OrderStatsResponse.java
│       └── TopProductResponse.java
├── ai/                                  (Phase 10)
│   ├── AiService.java
│   ├── AiController.java
│   └── dto/
│       └── AiRequest.java
└── common/
    └── exception/
        ├── ResourceNotFoundException.java
        ├── InvalidStatusTransitionException.java
        └── GlobalExceptionHandler.java
```

---

## DB Migrations
- V1: tenants table + 2 seed tenants (tenant-1, tenant-2)
- V2: users table
- V3: added primary key + updated_at to users
- V4: products table
- V5: orders + order_items tables
- V6: payments table
- V7: subscriptions table (cancelledAt, updatedAt columns)
- V8: notifications table (is_read, read_at, updated_at columns)

---

## API Endpoints

### Auth (public)
- POST /auth/register → register user
- POST /auth/login    → login, returns JWT token
- POST /auth/logout   → logout, blacklist token (Phase 9)

### Products (protected)
- GET    /api/products       → all active products
- GET    /api/products/{id}  → single product
- POST   /api/products       → create (STORE_OWNER)
- PUT    /api/products/{id}  → update (STORE_OWNER)
- DELETE /api/products/{id}  → soft delete (STORE_OWNER)

### Orders (protected)
- POST   /api/orders              → create order (CUSTOMER)
- GET    /api/orders/my           → my orders (CUSTOMER)
- GET    /api/orders/{id}         → single order (CUSTOMER + STORE_OWNER)
- GET    /api/orders              → all orders (STORE_OWNER)
- PATCH  /api/orders/{id}/status  → update status (STORE_OWNER)

### Payments (protected)
- POST   /api/payments                 → initiate (CUSTOMER)
- PATCH  /api/payments/{id}/complete   → simulate success (CUSTOMER)
- PATCH  /api/payments/{id}/fail       → simulate failure (CUSTOMER)
- GET    /api/payments/order/{orderId} → payment status (CUSTOMER + STORE_OWNER)

### Subscriptions (protected)
- GET  /api/subscriptions          → current plan (STORE_OWNER)
- POST /api/subscriptions          → subscribe (STORE_OWNER)
- PUT  /api/subscriptions/upgrade  → upgrade plan (STORE_OWNER)
- PUT  /api/subscriptions/cancel   → cancel plan (STORE_OWNER)

### Notifications (protected)
- GET /api/notifications            → my notifications (authenticated)
- PUT /api/notifications/{id}/read  → mark as read (authenticated)

### Analytics (protected)
- GET /api/analytics/revenue   → revenue stats (STORE_OWNER)
- GET /api/analytics/orders    → order stats (STORE_OWNER)
- GET /api/analytics/products  → top products (STORE_OWNER)

### AI (Phase 10)
- POST /api/ai/description → generate product description (STORE_OWNER)

---

## Status Flows

### Order Status Flow
```
PENDING → CONFIRMED → SHIPPED → DELIVERED (locked)
PENDING → CANCELLED (locked)
CONFIRMED → CANCELLED (locked)
DELIVERED → locked (no changes)
CANCELLED → locked (no changes)
```

### Payment Status Flow
```
PENDING → COMPLETED → Order auto CONFIRMED
PENDING → FAILED    → Stock restored + Order CANCELLED
COMPLETED → REFUNDED (future)
```

### Subscription Status Flow
```
(none) → ACTIVE (subscribe)
ACTIVE → CANCELLED (cancel)
ACTIVE → ACTIVE (upgrade — FREE→BASIC→PREMIUM only)
CANCELLED → ACTIVE (resubscribe)
ACTIVE → EXPIRED (auto — scheduled job Phase 9)
```

---

## Key Architecture Decisions
- Layered architecture (Controller → Service → Repository)
- Soft delete for products (active = false)
- Price snapshot in order_items (locked at purchase time)
- Stock deduction on order creation
- Stock restoration on payment failure
- customerId/email from JWT (never from request body)
- tenantId from TenantContext (never from request body)
- @Transactional on all write operations
- @Transactional(readOnly = true) on all read operations
- BigDecimal for all money fields (never double/float)
- Status transition validation in OrderService
- Separate Request/Response DTOs per module
- Mapper classes for entity ↔ DTO conversion
- GlobalExceptionHandler for consistent error responses
- @Getter @Setter @NoArgsConstructor on JPA entities (avoid @Data)
- @Data only on DTOs
- @RequiredArgsConstructor on Services (final field injection)
- Typed exceptions → proper HTTP status codes
- Email failure never breaks core business flow (try/catch)
- One payment per order (DB unique constraint + app check)
- One subscription per tenant (DB unique constraint)
- computed fields (expired) never stored in DB
- Boolean (wrapper) not boolean (primitive) on JPA entities
- Analytics queries in existing repositories — no duplication

---

## Bugs Fixed During Development
- OrderItem.ProductName → productName (capital P broke Lombok setter)
- Order/OrderItem @Data → @Getter @Setter (JPA dirty checking)
- findByIdAndTenantId returning List → fixed to Optional
- findByTenantIdAndStatus String → fixed to OrderStatus type
- java.nio.file.AccessDeniedException → wrong import
- Ownership check bug: order.getCustomerId().equals(order.getCustomerId())
  → fixed to currentUser.getId()
- completePayment missing orderRepository.save(order)
- Payment.java column update_at → updated_at
- Raw RuntimeException → IllegalArgumentException / typed exceptions
- Redundant flush() + findById().get() → use saved entity directly
- Wrong ErrorResponse (Spring interface) → Map<String, Object> with buildError()
- java.awt.print.Pageable → org.springframework.data.domain.Pageable
- Duplicate OrderRepository in analytics → deleted, methods added to
  existing order/OrderRepository.java
- JPQL (SUM(),0) syntax → COALESCE(SUM(),0)
- JPQL MONTH()/YEAR() → FUNCTION('MONTH')/FUNCTION('YEAR')
- JPQL status string 'DELIVERED' → full enum path

---

## Required Headers for Every Protected Request
```
Authorization: Bearer <jwt_token>
X-Tenant-ID:   tenant-1
Content-Type:  application/json
```

---

## Test Data
- Tenant IDs: tenant-1, tenant-2
- CUSTOMER: adarsh@test.com
- STORE_OWNER: owner1@test.com

## Postman Collection Structure
```
ShopSync AI/
├── Auth/
│   ├── Register
│   ├── Login (CUSTOMER)
│   ├── Login (STORE_OWNER)
│   └── Logout (Phase 9)
├── Products/
│   ├── Get All Products
│   ├── Get Product by ID
│   ├── Create Product
│   ├── Update Product
│   └── Delete Product
├── Orders/
│   ├── Create Order
│   ├── Get My Orders
│   ├── Get Order by ID
│   ├── Get All Orders
│   └── Update Order Status
├── Payments/
│   ├── Initiate Payment
│   ├── Complete Payment
│   ├── Fail Payment
│   └── Get Payment by Order
├── Subscriptions/
│   ├── Get Current Plan
│   ├── Subscribe (FREE)
│   ├── Upgrade (BASIC)
│   ├── Upgrade (PREMIUM)
│   ├── Try Downgrade (should 400)
│   ├── Cancel
│   └── Resubscribe after Cancel
├── Notifications/
│   ├── Get My Notifications
│   └── Mark as Read
└── Analytics/
    ├── Revenue Stats
    ├── Order Stats
    └── Top Products
```

---

## application.yml Config Reference
```yaml
spring:
  application:
    name: ShopSync-AI
  datasource:
    url: jdbc:postgresql://localhost:5432/saas_ecommerce
    username: saas_user
    password: saas_pass
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
  mail:
    host: smtp.gmail.com
    port: 587
    username: your-gmail@gmail.com
    password: your-16-char-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
  data:
    redis:
      host: localhost
      port: 6379
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

---

## Docker Reference
```bash
# Start containers
docker-compose up -d

# Check containers
docker ps

# Connect to PostgreSQL
docker exec -it saas_postgres psql -U saas_user -d saas_ecommerce

# Useful SQL
SELECT * FROM orders;
SELECT * FROM payments;
SELECT * FROM notifications;
DELETE FROM payments;
DELETE FROM order_items;
DELETE FROM orders;

# Exit psql
\q
```

---

## Git Commit Convention
```
feat:     new feature
fix:      bug fix
refactor: code improvement
docs:     documentation
chore:    config/dependencies
```

## Project Status
```
Phase 1  ✅ Multi-tenancy
Phase 2  ✅ Auth
Phase 3  ✅ Products
Phase 4  ✅ Orders
Phase 5  ✅ Payments
Phase 6  ✅ Subscriptions
Phase 7  ✅ Notifications + Email
Phase 8  ✅ Analytics
Phase 9  🔜 Swagger + Redis + Pagination
Phase 10 🔜 AI Features (OpenAI)
Phase 11 🔜 Razorpay + Kafka + Tests
Phase 12 🔜 React Frontend
```