package com.saas.ecommerce.order;

import com.saas.ecommerce.auth.Role;
import com.saas.ecommerce.auth.User;
import com.saas.ecommerce.auth.UserRepository;
import com.saas.ecommerce.common.exception.InvalidStatusTransitionException;
import com.saas.ecommerce.common.exception.ResourceNotFoundException;
import com.saas.ecommerce.order.dto.OrderItemRequest;
import com.saas.ecommerce.order.dto.OrderRequest;
import com.saas.ecommerce.order.dto.OrderResponse;
import com.saas.ecommerce.product.Product;
import com.saas.ecommerce.product.ProductRepository;
import com.saas.ecommerce.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    // ─── Auth Helper ─────────────────────────────────────────────────────────
    //get current user details
    private User getCurrentUser() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found" + email));
    }
//-----------------------------Create order -------------------------------------------------------------
    // Creat order
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        validateOrderRequest(request);
        String tenantId = TenantContext.getTenantId();
        User user = getCurrentUser();
        List<Product> products = fetchAndValidateProducts(request, tenantId);
        Order order = buildOrder(user, tenantId);
        BigDecimal total = processItems(request, products, order);
        order.setTotalAmount(total);
        Order saved = orderRepository.save(order);
        orderRepository.flush();
        log.info("Order created: {} for customer: {} in tenant: {}",
                saved.getId(),
                user.getEmail(),
                tenantId);
        return orderMapper.toResponse(orderRepository.findById(saved.getId()).get());
    }
   // All helper method-------------------------------------------------------------
    //validate Stock
    private void validateOrderRequest(OrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Order must have at least one item");
        }
    }
    // find all order product & validate it.
    private List<Product> fetchAndValidateProducts(OrderRequest request, String tenantId) {

        return request.getItems().stream()
                .map(item -> productRepository.findByIdAndTenantId(item.getProductId(), tenantId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Product not found"
                                        + item.getProductId())))
                .toList();

    }
    //build Empty order
    private Order buildOrder(User user, String tenantId) {

        Order order = new Order();
        order.setCustomerId(user.getId());
        order.setCustomerEmail(user.getEmail());
        order.setTenantId(tenantId);
        order.setStatus(OrderStatus.PENDING);
        order.setItems(new ArrayList<>());
        return order;
    }
    //Process all items to get total
    private BigDecimal processItems(OrderRequest request,
                                    List<Product> products,
                                    Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < request.getItems().size(); i++) {

            OrderItemRequest itemRequest = request.getItems().get(i);
            Product product = products.get(i);
            validateStock(product, itemRequest.getQuantity());
            BigDecimal unitPrice = product.getPrice();
            BigDecimal totalPrice = unitPrice.multiply(
                    BigDecimal.valueOf(itemRequest.getQuantity()));
            OrderItem item = buildOrderItem(order,
                    product, itemRequest.getQuantity(), unitPrice, totalPrice);
            // item = { productName:"iPhone 15", qty:2, unitPrice:$999, totalPrice:$1998 }
            order.getItems().add(item);
            // Add item to order's list
            deductStock(product, itemRequest.getQuantity());
            total = total.add(totalPrice);
        }
        return total;
    }
    //validateStock
    private void validateStock(Product product, int requestQty) {
        if (product.getStockQuantity() < requestQty) {
            throw new RuntimeException("Insufficient stock for product:"
                    + product.getName()
                    + "requested:" + requestQty);
        }

    }
    //buildOrderItem  : Build a single order item
    private OrderItem buildOrderItem(Order order,
                                     Product product,
                                     int quantity,
                                     BigDecimal unitPrice,
                                     BigDecimal totalPrice) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProductId(product.getId());
        orderItem.setProductName(product.getName());
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(unitPrice);
        orderItem.setTotalPrice(totalPrice);
        return orderItem;
    }
    //deduct stock
    private void deductStock(Product product, int quantity) {
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);
        log.info("Stock deducted for product: {}. Remaining: {}",
                product.getName(),
                product.getStockQuantity());
    }
// ─── READ OPERATIONS ──────────────────────────────────────────────────────
    // CUSTOMER — Get my orders
    @Transactional
    public List<OrderResponse> getMyOrders() {
        String tenantId = TenantContext.getTenantId();
        User user = getCurrentUser();
        return orderRepository.findByCustomerIdAndTenantId(user.getId(), tenantId)
                .stream()
                .map(orderMapper::toResponse)
                .toList();

    }
    //    get order
    @Transactional
    public OrderResponse getOrder(String id) {

        String tenantId = TenantContext.getTenantId();
        User currentUser = getCurrentUser();
        Order order = orderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + id));
        // CUSTOMER can only see their own orders
        if (currentUser.getRole() == Role.CUSTOMER
                && !order.getCustomerId().equals(order.getCustomerId())) {

            throw new AccessDeniedException("you do not have access to this order");
        }
        return orderMapper.toResponse(order);
    }
    //getAllOrders
    @Transactional
    public List<OrderResponse> getAllOrders() {
        String tenantId = TenantContext.getTenantId();
        log.info("Fetching all orders for tenant: {}", tenantId);
        return orderRepository.findByTenantId(tenantId)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }
//------UPDATE STATUS-------------------------------------------------------
    public OrderResponse updateStatus(String id, OrderStatus newStatus) {
        String tenantId = TenantContext.getTenantId();

        Order order= orderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + id));

        validateStatusTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        Order saved=orderRepository.save(order);

        log.info("Order {} status updated: {} → {} | tenant: {}",
                id, order.getStatus(), newStatus, tenantId);
        return orderMapper.toResponse(saved);
    }

    //PRIVATE HELPERS
    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid= switch(current){
            case PENDING -> next == OrderStatus.CONFIRMED ||  next == OrderStatus.CANCELLED;
            case CONFIRMED -> next == OrderStatus.SHIPPED
                    || next == OrderStatus.CANCELLED;
            case SHIPPED -> next ==   OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!valid) {
            throw new InvalidStatusTransitionException(current, next);
        }
    }
}
