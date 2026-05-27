package com.saas.ecommerce.order;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "customer_id",nullable = false)
    private String customerId;

    @Column(name = "customer_email",nullable = false)
    private String customerEmail;

    @Column(name = "tenant_id",nullable = false)
    private  String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount",nullable = false,precision =  10,scale = 2)
    private BigDecimal totalAmount;

    //one Oder has Many items
    //cascade ==if order saved/deleted from list -> delete from db
    //cascade = CascadeType.ALL  automatically saves all items too, no need to save each item separately.
    //orphanRemoval = if item remove from list -->it get delete from db also
    //ArrayList intialized - avoids NullPointerException when adding items
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL ,orphanRemoval = true)
    private List<OrderItem> items= new ArrayList<>();


    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @LastModifiedDate
    @Column(name="updated_at")
    private LocalDateTime updatedAt;





}
