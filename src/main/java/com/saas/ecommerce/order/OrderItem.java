package com.saas.ecommerce.order;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@Entity
@Table(name="order_items")
public class OrderItem  {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private String id;

    // Many items belong to one order
    // LAZY = don't load Order data unless explicitly needed
    // Avoids unnecessary DB queries
   // EAGER would load entire Order every time you fetch an item
    @ToString.Exclude// break the infinite loop
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="order_id",nullable = false)//it is the fK in db
    private Order order;

    @Column(nullable = false)
    private String productId;

    //Snapshot of product name at time of order
    @Column(name="product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;


    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false ,precision = 10, scale = 2)
    private BigDecimal totalPrice;


}
