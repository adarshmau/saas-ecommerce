package com.saas.ecommerce.product;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name="products")
@EntityListeners(AuditingEntityListener.class)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false,precision =  10,scale = 2)
    private BigDecimal price;

    @Column(name ="stock_quantity",nullable = false)
    private Integer  stockQuantity =0;

    private String category;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "tenant_id",nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp  //sets the field value to the current timestamp when the entity is first saved
    @Column(name ="created_at" ,updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp    // It automatically sets the field value to the current timestamp on each entity update.
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
