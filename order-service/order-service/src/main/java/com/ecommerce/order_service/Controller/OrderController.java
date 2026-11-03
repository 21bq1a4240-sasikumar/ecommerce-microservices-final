
package com.ecommerce.order_service.Controller;

import com.ecommerce.order_service.DTO.OrderRequest;
import com.ecommerce.order_service.Entity.Order;
import com.ecommerce.order_service.Service.OrderService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> placeOrder(
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey,
            @Valid @RequestBody OrderRequest request) {

        Order order = orderService.placeOrder(request, idempotencyKey);

        return ResponseEntity.ok(order);
    }
}
