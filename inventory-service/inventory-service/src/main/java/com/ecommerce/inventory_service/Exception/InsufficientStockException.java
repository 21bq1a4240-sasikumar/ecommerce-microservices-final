package com.ecommerce.inventory_service.Exception;

public class InsufficientStockException extends  RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
