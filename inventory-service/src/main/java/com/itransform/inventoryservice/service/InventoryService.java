package com.itransform.inventoryservice.service;

import com.itransform.inventoryservice.dto.*;
import com.itransform.inventoryservice.exception.ResourceNotFoundException;
import com.itransform.inventoryservice.model.Inventory;
import com.itransform.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryResponse addInventory(InventoryRequest request) {
        Inventory inventory = Inventory.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .build();
        Inventory saved = inventoryRepository.save(inventory);
        return mapToResponse(saved);
    }

    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public InventoryResponse getInventoryByProductId(String productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        return mapToResponse(inventory);
    }

    public InventoryResponse updateInventory(String id, InventoryRequest request) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));
        inventory.setProductId(request.getProductId());
        inventory.setQuantity(request.getQuantity());
        Inventory updated = inventoryRepository.save(inventory);
        return mapToResponse(updated);
    }

    public void deleteInventory(String id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));
        inventoryRepository.delete(inventory);
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .quantity(inventory.getQuantity())
                .build();
    }
}
