package com.saas.ecommerce.notification.dto;

import com.saas.ecommerce.notification.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class NotificationResponse {
    private String id;
    private String orderId;
    private NotificationType type;
    private String message;
    private Boolean read;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
