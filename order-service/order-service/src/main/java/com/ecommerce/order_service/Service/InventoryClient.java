
package com.ecommerce.order_service.Service;

import com.ecommerce.order_service.Exception.InsufficientStockException;
import com.ecommerce.order_service.Exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@Service
public class InventoryClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    public InventoryClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Retry(name = "inventoryService")
    @CircuitBreaker(
            name = "inventoryService",
            fallbackMethod = "inventoryFallback"
    )
    @TimeLimiter(name = "inventoryService")
    public CompletableFuture<Void> reserveInventory(
            Long productId,
            int quantity) {

        System.out.println(
                "Calling Inventory Service for product "
                        + productId + ", quantity " + quantity
        );

        return webClientBuilder.build()
                .post()
                .uri(
                        inventoryServiceUrl
                                + "/api/inventory/{id}/reserve?quantity={quantity}",
                        productId,
                        quantity
                )
                .retrieve()
                .onStatus(
                        status -> status.value() == 409,
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    if (body.contains("Insufficient stock")) {
                                        return Mono.error(
                                                new InsufficientStockException(
                                                        "Insufficient stock for product "
                                                                + productId
                                                )
                                        );
                                    }

                                    return Mono.error(
                                            new RuntimeException(
                                                    "Inventory service error: " + body
                                            )
                                    );
                                })
                )
                .onStatus(
                        status -> status.value() == 404,
                        response -> Mono.error(
                                new WebClientResponseException(
                                        "Inventory record not found for product "
                                                + productId,
                                        404,
                                        "Not Found",
                                        null,
                                        null,
                                        null
                                )
                        )
                )
                .bodyToMono(String.class)
                .then()
                .toFuture();
    }


    private CompletableFuture<Void> inventoryFallback(
            Long productId,
            int quantity,
            Throwable throwable) {

        Throwable cause = throwable;

        while (cause != null) {

            // Preserve insufficient-stock errors (409).
            if (cause instanceof InsufficientStockException) {
                return CompletableFuture.failedFuture(cause);
            }

            // Preserve missing-inventory errors (404).
            if (cause instanceof WebClientResponseException ex
                    && ex.getStatusCode().value() == 404) {

                return CompletableFuture.failedFuture(
                        new com.ecommerce.order_service.Exception
                                .InventoryNotFoundException(
                                "Inventory not found for product " + productId
                        )
                );
            }

            cause = cause.getCause();
        }

        // Genuine Inventory Service failures remain 503.
        return CompletableFuture.failedFuture(
                new ServiceUnavailableException(
                        "Inventory service is currently unavailable"
                )
        );
    }

}

