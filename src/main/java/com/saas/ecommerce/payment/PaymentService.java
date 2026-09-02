package com.saas.ecommerce.payment;

import com.saas.ecommerce.auth.User;
import com.saas.ecommerce.auth.UserRepository;
import com.saas.ecommerce.common.exception.ResourceNotFoundException;
import com.saas.ecommerce.notification.NotificationService;
import com.saas.ecommerce.order.Order;
import com.saas.ecommerce.order.OrderRepository;
import com.saas.ecommerce.order.OrderStatus;
import com.saas.ecommerce.payment.dto.PaymentRequest;
import com.saas.ecommerce.payment.dto.PaymentResponse;
import com.saas.ecommerce.payment.dto.PaymentVerifyRequest;
import com.saas.ecommerce.product.Product;
import com.saas.ecommerce.product.ProductRepository;
import com.saas.ecommerce.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentMapper paymentMapper;
    private final NotificationService notificationService;
    private final RazorpayService razorpayService; // ✅ NEW

    // ─── Auth Helper ──────────────────────────────────────────

    private User getCurrentUser() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));
    }

    // ─── INITIATE PAYMENT ─────────────────────────────────────

    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {
        String tenantId = TenantContext.getTenantId();
        User user = getCurrentUser();

        // 1 — Fetch order
        Order order = orderRepository
                .findByIdAndTenantId(request.getOrderId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + request.getOrderId()));

        // 2 — Ownership check
        if (!order.getCustomerId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "You do not have access to this order");
        }

        // 3 — Status check
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Order is not in PENDING state. Current: "
                            + order.getStatus());
        }

        // 4 — Duplicate check
        paymentRepository.findByOrderIdAndTenantId(
                        request.getOrderId(), tenantId)
                .ifPresent(p -> {
                    throw new IllegalStateException(
                            "Payment already exists for order: "
                                    + request.getOrderId());
                });

        // 5 — Create Razorpay order ✅
        String razorpayOrderId = razorpayService
                .createRazorpayOrder(
                        order.getTotalAmount(),
                        "receipt_" + order.getId()
                                .substring(0, 8));

        // 6 — Save payment with razorpayOrderId
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setCustomerId(user.getId());
        payment.setTenantId(tenantId);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setRazorpayOrderId(razorpayOrderId); // ✅

        Payment saved = paymentRepository.save(payment);

        log.info("Payment initiated: {} razorpayOrderId: {}",
                saved.getId(), razorpayOrderId);

        return paymentMapper.toResponse(saved);
    }

    // ─── VERIFY PAYMENT ───────────────────────────────────────

    @Transactional
    public PaymentResponse verifyPayment(
            PaymentVerifyRequest request) {
        String tenantId = TenantContext.getTenantId();

        // 1 — Find payment by razorpayOrderId
        Payment payment = paymentRepository
                .findByRazorpayOrderIdAndTenantId(
                        request.getRazorpayOrderId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for Razorpay order: "
                                + request.getRazorpayOrderId()));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment already processed. Status: "
                            + payment.getStatus());
        }

        // 2 — Verify signature ✅
        boolean isValid = razorpayService.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        if (isValid) {
            // 3 — Payment success
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setRazorpayPaymentId(
                    request.getRazorpayPaymentId());
            payment.setRazorpaySignature(
                    request.getRazorpaySignature());

            // 4 — Auto confirm order
            Order order = orderRepository
                    .findByIdAndTenantId(
                            payment.getOrderId(), tenantId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Order not found"));
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            // 5 — Send notification
            notificationService.sendPaymentNotification(
                    payment.getCustomerId(),
                    tenantId,
                    payment.getOrderId(),
                    PaymentStatus.COMPLETED);

            log.info("Payment verified successfully: {}",
                    payment.getId());

        } else {
            // 3 — Payment failed — invalid signature
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(
                    "Invalid payment signature");

            // 4 — Restore stock + cancel order
            restoreStock(payment.getOrderId(), tenantId);

            // 5 — Send notification
            notificationService.sendPaymentNotification(
                    payment.getCustomerId(),
                    tenantId,
                    payment.getOrderId(),
                    PaymentStatus.FAILED);

            log.warn("Payment verification failed — " +
                    "invalid signature: {}", payment.getId());
        }

        return paymentMapper.toResponse(
                paymentRepository.save(payment));
    }

    // ─── GET PAYMENT BY ORDER ─────────────────────────────────

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrder(String orderId) {
        String tenantId = TenantContext.getTenantId();
        return paymentRepository
                .findByOrderIdAndTenantId(orderId, tenantId)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for order: " + orderId));
    }

    // ─── RESTORE STOCK ────────────────────────────────────────

    private void restoreStock(String orderId, String tenantId) {
        Order order = orderRepository
                .findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found: " + orderId));

        order.getItems().forEach(item -> {
            Product product = productRepository
                    .findByIdAndTenantId(
                            item.getProductId(), tenantId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Product not found: "
                                            + item.getProductId()));

            product.setStockQuantity(
                    product.getStockQuantity()
                            + item.getQuantity());
            productRepository.save(product);

            log.info("Stock restored for product: {}. New: {}",
                    product.getName(),
                    product.getStockQuantity());
        });

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}