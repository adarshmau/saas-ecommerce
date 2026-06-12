package com.saas.ecommerce.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification,String> {


    // Get all notifications for a user in a tenant
    List<Notification> findByUserIdAndTenantIdOrderByCreatedAtDesc(String userId,String tenantId);


    // get unread notification
    List<Notification> findByUserIdAndTenantIdAndReadFalse(String  userId,String tenantId);


    // Get single notification with security check
    Optional<Notification> findByIdAndTenantId(
            String id, String tenantId);



}
