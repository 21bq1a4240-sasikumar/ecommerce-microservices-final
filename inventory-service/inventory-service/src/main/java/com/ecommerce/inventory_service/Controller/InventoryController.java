package com.ecommerce.inventory_service.Controller;

import com.ecommerce.inventory_service.Entity.Inventory;
import com.ecommerce.inventory_service.Service.InventoryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService){
        this.inventoryService=inventoryService;
    }
    @PostMapping("/{productId}/add")
    public Inventory addStock(
            @PathVariable Long productId,
            @RequestParam int quantity){
        return inventoryService.addStock(productId,quantity);
    }
    @GetMapping("/{productId}")
    public Inventory getStock(
            @PathVariable Long productId) {

        return inventoryService.getStock(productId);
    }
    @PostMapping("/{productId}/reserve")
    public Inventory reserveStock(
            @PathVariable Long productId,
            @RequestParam int quantity) throws InterruptedException {


        return inventoryService.reserveStock(productId, quantity);
    }
    @PostMapping("/{productId}/release")
    public Inventory releaseStock(
            @PathVariable Long productId,
            @RequestParam int quantity) {

        return inventoryService.releaseStock(productId, quantity);
    }

}
