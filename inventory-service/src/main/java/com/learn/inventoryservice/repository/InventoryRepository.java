package com.learn.inventoryservice.repository;

import com.learn.inventoryservice.model.Inventory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface InventoryRepository extends MongoRepository<Inventory, String> {
     Optional<Inventory> findByProductId(String productId);
}
