AGENTS.md - SaaS Ecommerce Codebase Guide
Architecture Overview

Multi-tenant SaaS ecommerce platform** (Java 22, Spring Boot 3.5.14, PostgreSQL 15, Flyway migrations)

Core Design Principle: Tenant Isolation via ThreadLocal
Every HTTP request runs on its own thread. Tenant ID is extracted from JWT token → stored in `TenantContext` (ThreadLocal) → automatically available throughout the request lifecycle. **NEVER accept tenantId from request body; ALWAYS use `TenantContext.getTenantId()`.**

```
Request Flow: JwtFilter → extract token → TenantContext.setTenantId() → SecurityContext auth → Service layer

## Project Structure & Module Responsibilities

| Module | Purpose | Key Files |
|--------|---------|-----------|
| `auth/` | User registration, login, JWT tokens | User.java, Role enum, AuthService, JwtService |
| `config/` | Security, filters, database config | SecurityConfig, JwtFilter, DataSourceConfig |
| `tenant/` | Multi-tenancy context management | TenantContext.java (ThreadLocal holder) |
| `product/` | Product CRUD with soft delete | Product entity, ProductService (@Transactional) |
| `order/` | Order management, status transitions | Order/OrderItem entities, OrderService (complex logic) |
| `payment/`, `subscription/`, `notification/` | Phase 5+ modules (in progress) | Follow product/order patterns |
| `common/exception/` | Global exception handling | GlobalExceptionHandler, custom exception classes |

---

## Critical Workflows & Commands

### Setup & Database
```bash
# Start PostgreSQL + Redis (Docker)
docker-compose up

# Build & run application
mvnw clean install
mvnw spring-boot:run
```

DB Migrations**: Flyway auto-runs `.sql` files in `src/main/resources/db/migration/` on startup.
DO NOT use Hibernate DDL (`ddl-auto: none`)
Always create `V##__description.sql` migrations before adding entity fields

Test Data 
Tenants**: `tenant-1`, `tenant-2` (seeded in V1)
Test Users**: Email/password stored in encrypted form in DB
Postman Headers**: `Authorization: Bearer <JWT>`, `X-Tenant-ID: tenant-1`, `Content-Type: application/json`

Deployment Info 
Server Port**: 8080 (in `application.yml`)
Timezone**: Asia/Kolkata (hardcoded in `EcommerceApplication.java` main method)
JWT Secret**: 64-char hex in `application.yml` → `${application.jwt.secret}`

---

## Core Patterns & Conventions

1. **Tenant Isolation Pattern** (CRITICAL)
Every service method that touches data MUST extract tenant ID:
```java
String tenantId = TenantContext.getTenantId();  // NEVER from request body
List<Product> products = productRepository.findByTenantIdAndActiveTrue(tenantId);
```

Repository queries ALWAYS filter by `tenantId`:
```java
// ProductRepository
List<Product> findByTenantIdAndActiveTrue(String tenantId);
Optional<Product> findByIdAndTenantId(String id, String tenantId);
```

### 2. **Service Layer Transactions**
All **write operations** (create/update/delete) MUST use `@Transactional`:
```java
@Transactional
public ProductResponse createProduct(ProductRequest request) {
    Product saved = productRepository.save(productMapper.toEntity(request, tenantId));
    productRepository.flush();  // Force flush before re-fetch
    return productMapper.toResponse(productRepository.findById(saved.getId()).get());
}
```

**Why flush & re-fetch?** Ensures @CreatedAt, @UpdatedAt timestamps are populated before DTO mapping.

### 3. **Money Fields: Always BigDecimal**
```java
// ✅ CORRECT
private BigDecimal price;
private BigDecimal totalAmount;

// ❌ WRONG - Never use double/float
private double price;  // Can have floating-point precision errors
```

### 4. **Soft Delete Pattern** (Products)
```java
// Product entity has 'active' boolean field (default true)
@Column(name = "active", nullable = false)
private boolean active = true;

// When deleting: set active = false
//@Transactional
//public void deleteProduct(String id) {
//    Product product = productRepository.findByIdAndTenantId(id, tenantId)...
//    product.setActive(false);  // Soft delete
//    productRepository.save(product);
//}

// Queries filter: findByTenantIdAndActiveTrue(tenantId)
```

### 5. **DTOs: Separate Request/Response Classes**
Never expose internal entity fields directly. Each module has distinct DTOs:
```java
// ProductRequest - Input validation
//public class ProductRequest {
//    @NotBlank private String name;
//    @Valid @NotNull private BigDecimal price;
//    // NO tenantId field - comes from TenantContext
//}

// ProductResponse - Output (no tenantId exposed)
public class ProductResponse {
    public String id;
    public String name;
    public BigDecimal price;
    // List only fields safe for client consumption
}

// Mapper converts entity ↔ DTO
public Product toEntity(ProductRequest request, String tenantId) {
    Product p = new Product();
    p.setName(request.getName());
    p.setTenantId(tenantId);  // Inject here
    return p;
}
```

### 6. **Current User Extraction** (In services requiring user context)
```java
private User getCurrentUser() {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
}
```

### 7. **Role-Based Authorization**
Controller methods use `@PreAuthorize` for role checks:
```java
@PostMapping()
@PreAuthorize("hasRole('STORE_OWNER')")  // Checks if user.role == STORE_OWNER
public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(req));
}
```

Roles enum: `ADMIN`, `STORE_OWNER`, `CUSTOMER` (in `auth/Role.java`)

### 8. **Exception Handling**
Global handler in `common/exception/GlobalExceptionHandler.java`:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }
    // ... more handlers
}
```

**Custom exceptions** (extend RuntimeException):
- `ResourceNotFoundException` → HTTP 404
- `InvalidStatusTransitionException` → HTTP 400
- `AccessDeniedException` (Spring Security) → HTTP 403

### 9. **Logging Pattern**
Use Lombok's `@Slf4j` annotation:
```java
//@Slf4j
//@Service
//public class ProductService {
//    public List<ProductResponse> getAllProducts() {
//        String tenantId = TenantContext.getTenantId();
//        log.info("Fetching all products for tenant: {}", tenantId);  // Structured logging
//        return productRepository.findByTenantIdAndActiveTrue(tenantId)...
//    }
//}
```

---

## API Endpoint Patterns

### Public Endpoints (No JWT Required)
```
POST /auth/register    → Register user (body: name, email, password, tenantId, role)
POST /auth/login       → Login (body: email, password) → Returns JWT token
GET  /actuator/**      → Health checks
```

### Protected Endpoints (JWT Required)
All require header: `Authorization: Bearer <token>` and `X-Tenant-ID: tenant-1`

**Products** (READ access for all authenticated users):
```
GET  /api/products         → List all tenant products
GET  /api/products/{id}    → Get single product
POST /api/products         → Create (STORE_OWNER only)
PUT  /api/products/{id}    → Update (STORE_OWNER only)
DELETE /api/products/{id}  → Soft delete (STORE_OWNER only)
```

**Orders** (Complex role/owner logic):
```
POST /api/orders                  → Create order (CUSTOMER)
GET  /api/orders/my-orders        → My orders (CUSTOMER)
GET  /api/orders/{id}             → Get order (must be order owner or STORE_OWNER)
GET  /api/orders                  → All tenant orders (STORE_OWNER)
PATCH /api/orders/{id}/status     → Update status (STORE_OWNER only)
```

---

## Status Transition Flows

### Order Status Transitions (Validated in OrderService)
```
PENDING → CONFIRMED → SHIPPED → DELIVERED
  ↓
CANCELLED (can transition from PENDING or CONFIRMED)
```

**Implementation**: `OrderService.updateStatus()` validates transitions in `isValidTransition()` helper.

### Payment Status Transitions
```
PENDING → COMPLETED
  ↓
FAILED
COMPLETED → REFUNDED
```

---

## Common Agent Tasks & Patterns

### Adding a New Entity & CRUD Endpoint
1. **Create Entity** in new module (e.g., `src/main/java/.../feature/Feature.java`)
   - Use Lombok: `@Getter @Setter @NoArgsConstructor`
   - Include `tenantId` field for multi-tenancy
   - Use `@Entity @Table(name="features")`

2. **Create Repository** (e.g., `FeatureRepository.java extends JpaRepository`)
   ```java
   List<Feature> findByTenantId(String tenantId);
   Optional<Feature> findByIdAndTenantId(String id, String tenantId);
   ```

3. **Create DTOs** in `feature/dto/` (FeatureRequest, FeatureResponse)

4. **Create Mapper** (e.g., `FeatureMapper.java`)
   ```java
   public Feature toEntity(FeatureRequest req, String tenantId) {
       Feature f = new Feature();
       f.setName(req.getName());
       f.setTenantId(tenantId);
       return f;
   }
   ```

5. **Create Service** with `@Transactional` write methods
   ```java
   @Slf4j @Service @RequiredArgsConstructor
   public class FeatureService {
       private final FeatureRepository repo;
       
       @Transactional
       public FeatureResponse create(FeatureRequest req) {
           String tenantId = TenantContext.getTenantId();
           Feature saved = repo.save(mapper.toEntity(req, tenantId));
           repo.flush();
           return mapper.toResponse(repo.findById(saved.getId()).get());
       }
   }
   ```

6. **Create Controller** with `@RestController @RequestMapping("/api/features")`
   - Use `@PreAuthorize` for role checks
   - Return `ResponseEntity.status(HttpStatus.CREATED).body(...)` for creates

7. **Create Migration** (e.g., `V9__create_features_table.sql`)
   - Always include `tenant_id VARCHAR(36) REFERENCES tenants(id)`

### Testing Connectivity
- Use Postman, cURL, or REST client plugin in IDE
- **Must include headers**: `Authorization: Bearer <token>`, `X-Tenant-ID: tenant-1`
- Invalid JWT or missing tenant ID → request fails silently in JwtFilter

---

## Important Gotchas & Decisions

| Issue | Solution |
|-------|----------|
| Tenant isolation broken | ❌ Never trust `request.getTenantId()` → Always use `TenantContext.getTenantId()` |
| Timestamp not populated | ❌ Forgot `@EnableJpaAuditing` on main app class OR forgot `productRepository.flush()` before re-fetch |
| Decimal precision errors | ❌ Using `double`/`float` for prices → Always use `BigDecimal` |
| Status transition fails silently | ✅ Check `OrderService.isValidTransition()` logic + add custom exception handling |
| JWT expires silently | ✅ Token expiry in milliseconds: `application.yml` → `application.jwt.expiration: 86400000` (24 hours) |
| Database constraint violated | Check Flyway migration versions at `/src/main/resources/db/migration/` |
| Role-based access denied | ✅ Verify `@PreAuthorize("hasRole('ROLE_NAME')")` matches Role enum; Spring adds `ROLE_` prefix |

---

## Developer Commands (PowerShell on Windows)

```powershell
# Clean rebuild
./mvnw.cmd clean install

# Run with hot reload
./mvnw.cmd spring-boot:run

# Run tests
./mvnw.cmd test

# Check for compilation errors only
./mvnw.cmd compile

# View dependency tree
./mvnw.cmd dependency:tree

# Run database migrations only (integrated with startup)
# Migrations run automatically on `spring-boot:run`
```

---

## Dependencies & Versions (Critical for Agents)

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.5.14 | Web framework |
| Java | 21 | Language (property in pom.xml) |
| PostgreSQL | 15 | Database |
| Flyway | Latest from Spring Boot | DB migrations |
| JJWT | 0.12.6 | JWT token generation/validation |
| Spring Security | 6.5 | Authentication/authorization (bundled with Boot 3.5.14) |
| Lombok | Latest from Boot | Boilerplate reduction (@Getter, @Setter, @Slf4j) |
| Spring Data JPA | Bundled with Boot | ORM/Repository pattern |

---

## Git & Code Style

- **Lombok conventions**: Use `@RequiredArgsConstructor` for DI (constructor injection)
- **Resource naming**: Entity names singular (User, Product), table names plural (users, products)
- **Field naming**: Use camelCase in Java, snake_case in SQL
- **Package structure**: One module per functional area (auth, product, order, etc.)
- **Exception hierarchy**: Always extend RuntimeException (unchecked) for business logic errors

---

## Known Phase Status

- **Phase 1 ✅**: Multi-tenancy framework (TenantContext, TenantFilter, DataSourceConfig)
- **Phase 2 ✅**: Auth module (JWT, User, Role, SecurityConfig)
- **Phase 3 ✅**: Product module (CRUD with soft delete)
- **Phase 4 🔄**: Order module (complex logic, status transitions) — partially complete
- **Phase 5 🔜**: Payment module (initiate, verify, refund)
- **Phase 6 🔜**: Subscription module (plans, upgrades, expiry checks)
- **Phase 7 ✅**: Notification module (event-driven, auto-triggered on order changes)
- **Phase 8 🔜**: Analytics module (revenue, top products)
- **Phase 9 🔜**: API improvements (pagination, search, rate limiting, versioning)

---

## Resources

- **Docker Setup**: See `docker-compose.yml` (PostgreSQL 15 + Redis 7)
- **DB Schemas**: See `src/main/resources/db/migration/V*.sql`
- **Entity Definitions**: See `src/main/java/.../entity/` classes
- **Configuration**: See `src/main/resources/application.yml` (JWT secret, DB creds, mail settings)
- **Test Data Setup**: All in Flyway V1 migration

