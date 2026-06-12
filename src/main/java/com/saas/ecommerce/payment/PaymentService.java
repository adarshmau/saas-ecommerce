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
import com.saas.ecommerce.product.Product;
import com.saas.ecommerce.product.ProductRepository;
import com.saas.ecommerce.tenant.TenantContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentMapper paymentMapper;
    private final NotificationService notificationService;


// Auth helper -----------------------------------------
    public User getCurrentUser() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));
    }
//  -------- INITIATE PAYMENT-------------------------------------------------------
    @Transactional
    public PaymentResponse initiatePayment(PaymentRequest request) {

        String tenantId= TenantContext.getTenantId();
        User user= getCurrentUser();

        //1 fetch the Order
        Order order= orderRepository
                .findByIdAndTenantId(request.getOrderId(),tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + request.getOrderId()));

       //2 Customer can only pay their own order
        if(!order.getCustomerId().equals(user.getId())){
            throw new AccessDeniedException(
                    "You do not have access to this order");
        }
        //3 Only PENDING orders can be paid
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Order is not in PENDING state. Current status:"
                    +order.getStatus());
        }
        // 4 Check payment doesn't already exist...
        paymentRepository.findByOrderIdAndTenantId(request.getOrderId(),tenantId)
                .ifPresent(p -> {
                    throw new IllegalStateException("Payment already exists for order: "
                            + request.getOrderId());
                });
        // 5 — Create payment
        Payment payment= new Payment();
        payment.setOrderId(order.getId());
        payment.setCustomerId(user.getId());
        payment.setTenantId(tenantId);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);

        Payment saved= paymentRepository.save(payment);

        log.info("Payment initiated:  {} for order: {} tenant: {}" ,
                saved.getId(),order.getId(),tenantId);
        return paymentMapper.toResponse(saved);

    }
    //COMPLETE PAYMENT (Simulate success)-----------------------------------------------------------
    @Transactional
    public PaymentResponse completePayment(String paymentId) {

        String tenantId= TenantContext.getTenantId();

        Payment payment= paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with id: " + paymentId));

        // Verify tenant
        if (!payment.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException(
                    "You do not have access to this order");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment is not PENDING. Current status: "
                            + payment.getStatus());
        }
        // Simulate payment reference (later → real Razorpay ID)
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentReference("PAY-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase());

        //Auto confirm this order
        Order order= orderRepository.findByIdAndTenantId(payment.getOrderId(),tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + payment.getOrderId()));

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // Send payment success notification
        notificationService.sendPaymentNotification(
                payment.getCustomerId(),
                tenantId,
                payment.getOrderId(),
                PaymentStatus.COMPLETED
        );
        Payment saved= paymentRepository.save(payment);
        log.info("Payment completed: {} order auto-confirmed: {}",saved.getId(),order.getId());
        return paymentMapper.toResponse(saved);
    }
    // ─── GET PAYMENT BY ORDER ------------------------------------------------------------------------
    public PaymentResponse getPaymentByOrder(String orderId) {

        String tenantId= TenantContext.getTenantId();
        return paymentRepository
                .findByOrderIdAndTenantId(orderId,tenantId)
                .map(paymentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));
    }
    //fails payments------------------------------------------------------------------------------------------
    @Transactional
    public PaymentResponse failPayment(String paymentId, String reason)
    {
        String tenantId= TenantContext.getTenantId();

        Payment payment=paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with id: " + paymentId));

        if (!payment.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException(
                    "You do not have access to this order");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Payment is not PENDING. Current status: "
                            + payment.getStatus());
        }
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);

        // Restore stock on failure
        restoreStock(payment.getOrderId(), tenantId);

       Payment saved = paymentRepository.save(payment);

        // Send payment failed notification
        notificationService.sendPaymentNotification(
                payment.getCustomerId(),
                tenantId,
                payment.getOrderId(),
                PaymentStatus.FAILED
        );

        log.info("Payment failed: {} reason: {}", saved.getId(), reason);

        return paymentMapper.toResponse(saved);
    }


    //helper method(RESTORE STOCK ON FAILURE)---------------------

    public void restoreStock(String orderId,String tenantId) {

        Order order= orderRepository.findByIdAndTenantId(orderId,tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));

        order.getItems().forEach(item -> {
            Product product = productRepository.findByIdAndTenantId(item.getProductId(),tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + item.getProductId()));

            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);


            log.info("Stock restored for product: {}. New stock: {}",
                    product.getName(), product.getStockQuantity());
        });
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

    }




}



