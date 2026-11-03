package com.ecommerce.inventory_service.Repository;

import com.ecommerce.inventory_service.Entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory,Long> {
    Optional<Inventory> findByProductId(Long productId);
}
