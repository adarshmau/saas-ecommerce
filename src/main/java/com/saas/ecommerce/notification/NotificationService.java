package com.saas.ecommerce.notification;

import com.saas.ecommerce.auth.User;
import com.saas.ecommerce.auth.UserRepository;
import com.saas.ecommerce.common.exception.ResourceNotFoundException;
import com.saas.ecommerce.notification.dto.NotificationResponse;
import com.saas.ecommerce.order.OrderStatus;
import com.saas.ecommerce.payment.PaymentStatus;
import com.saas.ecommerce.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final JavaMailSender mailSender;

    //    auth helper-------------------
    private User getCurrentUser() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    //SEND ORDER NOTIFICATION ---------------------------------------------------------
    @Transactional(propagation = Propagation.REQUIRED)
    public void sendOrderNotification(String UserId,
                                 String tenantId,
                                 String oderId,
                                 OrderStatus status) {
        //save to db
        Notification notification = new Notification();
        notification.setUserId(UserId);
        notification.setTenantId(tenantId);
        notification.setOrderId(oderId);
        notification.setType(mapStatusToType(status));
        notification.setMessage(buildOrderMessage(status, oderId));
        notification.setRead(false);
        notificationRepository.save(notification);

        // 2 — Send email
        User user= userRepository.findById(UserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + UserId));

        sendEmail(
                user.getEmail(),
                buildOrderSubject(status),
                buildOrderEmailBody(user.getName(), status, oderId));

        log.info("order notification sent to user {} for order {} with status {}",
                user.getEmail(), oderId, status);

    }

    //helper method------------------------------
    private NotificationType mapStatusToType(OrderStatus status) {
        return switch (status) {
            case PENDING -> NotificationType.ORDER_PLACED;
            case CONFIRMED -> NotificationType.ORDER_CONFIRMED;
            case SHIPPED -> NotificationType.ORDER_SHIPPED;
            case DELIVERED -> NotificationType.ORDER_DELIVERED;
            case CANCELLED -> NotificationType.ORDER_CANCELLED;
        };
    }

    //helper method-----------------------------------------------
    private String buildOrderMessage(OrderStatus status, String orderId) {
        String shortedId = orderId.substring(0, 8).toUpperCase();
        return switch (status) {
            case PENDING -> "Your order " + shortedId + " has been placed successfully.";
            case CONFIRMED -> "Your order " + shortedId + " has been confirmed.";
            case SHIPPED -> "Your order " + shortedId + " has been shipped.";
            case DELIVERED -> "Your order " + shortedId + " has been delivered.";
            case CANCELLED -> "Your order " + shortedId + " has been cancelled.";
        };
    }
    //helper method
    private String buildOrderSubject(OrderStatus status){
        return switch (status) {
            case PENDING -> "Order Placed successfully";
            case CONFIRMED -> "Your Order is  Confirmed";
            case SHIPPED -> "Order Shipped";
            case DELIVERED -> "Order Delivered";
            case CANCELLED -> "Order Cancelled";
        };
    }
    //helper method buildOrderEmailBody
    private String buildOrderEmailBody(String name,
                                       OrderStatus status,
                                       String orderId) {
        String shortId = orderId.substring(0, 8).toUpperCase();
        String greeting = "Hi " + name + ",\n\n";
        String footer = "\n\nThank you for shopping with us!\nSaaS Ecommerce Team";

        String body = switch (status) {
            case PENDING ->
                    "Your order #" + shortId
                            + " has been placed successfully.\n"
                            + "We will confirm it shortly.";
            case CONFIRMED ->
                    "Great news! Your order #" + shortId
                            + " has been confirmed.\n"
                            + "We are now preparing it for shipment.";
            case SHIPPED ->
                    "Your order #" + shortId
                            + " is on its way!\n"
                            + "You will receive it soon.";
            case DELIVERED ->
                    "Your order #" + shortId
                            + " has been delivered.\n"
                            + "We hope you enjoy your purchase!";
            case CANCELLED ->
                    "Your order #" + shortId
                            + " has been cancelled.\n"
                            + "If you have any questions, please contact our support.";
        };

        return greeting + body + footer;
    }
    //EMAIL SENDER
    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Sending email to {} with subject {}", to, subject);
        } catch (Exception e) {
            // Email failure NEVER breaks the order/payment flow
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }

    }
    //----------------------------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(){
        String tenantId= TenantContext.getTenantId();
        User user= getCurrentUser();
        return notificationRepository.findByUserIdAndTenantIdOrderByCreatedAtDesc(user.getId(), tenantId)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }
    // ─── MARK AS READ --------------------------------------------------------

    @Transactional
    public NotificationResponse markAsRead(String id) {
        String tenantId = TenantContext.getTenantId();
        User user = getCurrentUser();

        Notification notification = notificationRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification not found: " + id));

        if (!notification.getUserId().equals(user.getId())) {
            throw new AccessDeniedException(
                    "You do not have access to this notification");
        }

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now()); // ✅

        return notificationMapper.toResponse(
                notificationRepository.save(notification));
    }
    //Send Payment Notification----------------------------------------------------------
    @Transactional(propagation = Propagation.REQUIRED)
    public void sendPaymentNotification(String userId,
                                        String tenantId,
                                        String orderId,
                                        PaymentStatus status) {
        //save in db
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTenantId(tenantId);
        notification.setOrderId(orderId);
        notification.setType(status == PaymentStatus.COMPLETED
                ?NotificationType.PAYMENT_COMPLETED
                :NotificationType.PAYMENT_FAILED);
        notification.setMessage(buildPaymentMessage(status,orderId));
        notification.setRead(false);
        notificationRepository.save(notification);


        //send email
        User user= userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));

        sendEmail(
                user.getEmail(),
                buildPaymentSubject(status),
                buildPaymentEmailBody(user.getName(), status, orderId));

        log.info("Payment notification sent → user: {} order: {} status: {}",
                user.getEmail(), orderId, status);
    }
    //helper method----------------------------------------------------------------
    private String buildPaymentMessage(PaymentStatus status, String orderId) {
        String shortedId = orderId.substring(0, 8).toUpperCase();
        return switch (status) {
            case COMPLETED -> "Your payment for order " + shortedId + " has been completed successfully.";
            case FAILED -> "Unfortunately, your payment for order " + shortedId + " has failed.";
            default -> "Your payment for order " + shortedId + " has been updated.";
        };
    }

    //PAYMENT EMAIL CONTENT-------------
    private String buildPaymentSubject(PaymentStatus status) {
        return switch (status) {
            case COMPLETED -> "Payment Successful";
            case FAILED -> "Payment Failed";
            default -> "Payment status Update";
        };
    }

    private String buildPaymentEmailBody(String name,
                                         PaymentStatus status,
                                         String orderId) {
        String shortId = orderId.substring(0, 8).toUpperCase();
        String greeting = "Hi " + name + ",\n\n";
        String footer = "\n\nThank you for shopping with us!\nSaaS Ecommerce Team";
        String body=switch (status) {
            case COMPLETED -> "Your payment for order #" + shortId
                    + " has been completed successfully.\n"
                    + "Thank you for your purchase!";
            case FAILED -> "Unfortunately, your payment for order #" + shortId
                    + " has failed.\n"
                    + "Please try again or contact support.";
            default -> "Your payment for order #" + shortId+" has been updated.";
        };
        return greeting + body + footer;

    }

    //===========================================================================

}