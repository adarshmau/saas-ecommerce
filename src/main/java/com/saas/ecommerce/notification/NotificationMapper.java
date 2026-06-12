package com.saas.ecommerce.notification;

import com.saas.ecommerce.notification.dto.NotificationResponse;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification)
    {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setOrderId(notification.getOrderId());
        response.setType(notification.getType());
        response.setMessage(notification.getMessage());
        response.setRead(notification.getRead());
        response.setReadAt(notification.getReadAt());
        response.setCreatedAt(notification.getCreatedAt());
        response.setUpdatedAt(notification.getUpdatedAt());
        return response;
    }


}
