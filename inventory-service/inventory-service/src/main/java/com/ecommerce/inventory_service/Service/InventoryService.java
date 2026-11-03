package com.ecommerce.inventory_service.Service;

import com.ecommerce.inventory_service.Entity.Inventory;
import com.ecommerce.inventory_service.Exception.InsufficientStockException;
import com.ecommerce.inventory_service.Repository.InventoryRepository;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository){
        this.inventoryRepository=inventoryRepository;
    }
    public Inventory addStock(Long productId, int quantity) {

        Inventory inventory = inventoryRepository
                .findByProductId(productId)
                .orElseGet(() -> {
                    Inventory newInventory = new Inventory();
                    newInventory.setProductId(productId);
                    newInventory.setAvailableQuantity(0);
                    newInventory.setReservedQuantity(0);
                    return newInventory;
                });

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() + quantity
        );

        return inventoryRepository.save(inventory);
    }
    public Inventory getStock(Long productId) {
        return inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() ->
                        new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.NOT_FOUND,
                                "Inventory not found for product " + productId
                        )
                );
    }
    public Inventory reserveStock(Long productId, int quantity) {

        Inventory inventory = getStock(productId);

        if (inventory.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock for product " + productId
            );
        }

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() - quantity
        );

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() + quantity
        );

        return inventoryRepository.save(inventory);
    }
    public Inventory releaseStock(Long productId, int quantity) {

        Inventory inventory = getStock(productId);

        if (inventory.getReservedQuantity() < quantity) {
            throw new RuntimeException(
                    "Cannot release more than reserved stock"
            );
        }

        inventory.setReservedQuantity(
                inventory.getReservedQuantity() - quantity
        );

        inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() + quantity
        );

        return inventoryRepository.save(inventory);
    }
}
