package com.saas.ecommerce.auth;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name ="users")
public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID )
    private  String id;

    @Column(nullable = false)
    private  String name;

    @Column(nullable = false,unique=true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    @Column(name="tenant_id",nullable = false)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createAt;

    @UpdateTimestamp                             // tracks updates
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
