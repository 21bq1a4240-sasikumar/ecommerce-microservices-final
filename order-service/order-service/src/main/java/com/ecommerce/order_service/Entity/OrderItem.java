package com.ecommerce.order_service.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private int quantity;

    private double price;

    @ManyToOne
    @JoinColumn(name="order_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Order order;
}
