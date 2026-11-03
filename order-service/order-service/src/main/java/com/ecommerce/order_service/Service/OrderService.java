
package com.ecommerce.order_service.Service;
import com.ecommerce.order_service.Exception.InventoryNotFoundException;
import com.ecommerce.order_service.DTO.OrderItemRequest;
import com.ecommerce.order_service.DTO.OrderRequest;
import com.ecommerce.order_service.DTO.ProductResponse;
import com.ecommerce.order_service.Entity.Order;
import com.ecommerce.order_service.Entity.OrderItem;
import com.ecommerce.order_service.Entity.OrderStatus;
import com.ecommerce.order_service.Exception.InsufficientStockException;
import com.ecommerce.order_service.Exception.ProductNotFoundException;
import com.ecommerce.order_service.Exception.ServiceUnavailableException;
import com.ecommerce.order_service.Repository.OrderRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final InventoryClient inventoryClient;

    @Value("${catalog.service.url}")
    private String catalogServiceUrl;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    public OrderService(
            OrderRepository orderRepository,
            WebClient.Builder webClientBuilder,
            InventoryClient inventoryClient) {

        this.orderRepository = orderRepository;
        this.webClientBuilder = webClientBuilder;
        this.inventoryClient = inventoryClient;
    }

    public synchronized Order placeOrder(
            OrderRequest request,
            String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Idempotency-Key header is required and must not be blank"
            );
        }

        // Return the existing order if this key was already used.
        return orderRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> createOrder(request, idempotencyKey));
    }

    private Order createOrder(
            OrderRequest request,
            String idempotencyKey) {

        Order order = new Order();
        order.setIdempotencyKey(idempotencyKey);
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> orderItems = new ArrayList<>();

        try {

            // STEP 1: Check every product exists.
            for (OrderItemRequest itemRequest : request.getItems()) {

                ProductResponse product =
                        webClientBuilder.build()
                                .get()
                                .uri(
                                        catalogServiceUrl + "/api/products/{id}",
                                        itemRequest.getProductId()
                                )
                                .retrieve()
                                .onStatus(
                                        status -> status.value() == 404,
                                        response -> Mono.error(
                                                new ProductNotFoundException(
                                                        "Product not found: "
                                                                + itemRequest.getProductId()
                                                )
                                        )
                                )
                                .bodyToMono(ProductResponse.class)
                                .block();

                if (product == null) {
                    throw new ProductNotFoundException(
                            "Product not found: "
                                    + itemRequest.getProductId()
                    );
                }

                OrderItem item = new OrderItem();
                item.setProductId(product.getId());
                item.setQuantity(itemRequest.getQuantity());
                item.setPrice(product.getPrice());
                item.setOrder(order);

                orderItems.add(item);
            }

            // STEP 2: Reserve inventory.
            List<OrderItem> reservedItems = new ArrayList<>();

            try {

                for (OrderItem item : orderItems) {

                    inventoryClient.reserveInventory(
                            item.getProductId(),
                            item.getQuantity()
                    ).join();

                    reservedItems.add(item);
                }

            } catch (Exception inventoryException) {

                // Release any stock already reserved.
                for (OrderItem reservedItem : reservedItems) {

                    try {
                        webClientBuilder.build()
                                .post()
                                .uri(
                                        inventoryServiceUrl
                                                + "/api/inventory/{id}/release?quantity={quantity}",
                                        reservedItem.getProductId(),
                                        reservedItem.getQuantity()
                                )
                                .retrieve()
                                .bodyToMono(String.class)
                                .block();

                    } catch (Exception releaseException) {
                        System.err.println(
                                "Failed to release stock for product: "
                                        + reservedItem.getProductId()
                        );
                    }
                }

                // Unwrap CompletionException.
                Throwable cause = inventoryException;

                while ((cause instanceof CompletionException
                        || cause instanceof java.util.concurrent.ExecutionException
                        || cause instanceof RuntimeException)
                        && cause.getCause() != null
                        && cause != cause.getCause()) {

                    if (cause instanceof InventoryNotFoundException
                            || cause instanceof InsufficientStockException
                            || cause instanceof ServiceUnavailableException) {
                        break;
                    }

                    cause = cause.getCause();
                }

                if (cause instanceof ServiceUnavailableException) {
                    throw (ServiceUnavailableException) cause;
                }

                if (cause instanceof InsufficientStockException) {
                    throw (InsufficientStockException) cause;
                }

                if (cause instanceof InventoryNotFoundException) {
                    throw (InventoryNotFoundException) cause;
                }

                throw inventoryException;
            }

            // STEP 3: Calculate total.
            double total = 0;

            for (OrderItem item : orderItems) {
                total += item.getPrice() * item.getQuantity();
            }

            order.setItems(orderItems);
            order.setTotalAmount(total);

            // STEP 4: Save as CONFIRMED.
            order.setStatus(OrderStatus.CONFIRMED);

            return orderRepository.saveAndFlush(order);

        } catch (ServiceUnavailableException e) {

            order.setStatus(OrderStatus.FAILED);
            throw e;

        } catch (InsufficientStockException e) {

            order.setStatus(OrderStatus.FAILED);
            throw e;

        } catch (ProductNotFoundException e) {

            order.setStatus(OrderStatus.FAILED);
            throw e;

        }catch (InventoryNotFoundException e) {

            order.setStatus(OrderStatus.FAILED);
            throw e;

        } catch (DataIntegrityViolationException e) {

            // A database uniqueness conflict can occur if another
            // request uses the same key concurrently.
            return orderRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> e);

        } catch (Exception e) {

            order.setStatus(OrderStatus.FAILED);

            throw new RuntimeException(
                    "Order placement failed: " + e.getMessage(),
                    e
            );
        }
    }
}
