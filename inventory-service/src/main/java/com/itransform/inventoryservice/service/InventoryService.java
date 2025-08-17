package com.itransform.inventoryservice.service;

import com.itransform.inventoryservice.dto.*;
import com.itransform.inventoryservice.events.InventoryEvent;
import com.itransform.inventoryservice.exception.ResourceNotFoundException;
import com.itransform.inventoryservice.model.Inventory;
import com.itransform.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final EventPublisherService eventPublisherService;
    
    private static final int LOW_STOCK_THRESHOLD = 10;

    public InventoryResponse addInventory(InventoryRequest request) {
        Inventory inventory = Inventory.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .build();
        Inventory saved = inventoryRepository.save(inventory);
        
        // Publish inventory updated event
        publishInventoryEvent(saved, 0, InventoryEvent.INVENTORY_UPDATED);
        
        // Check if restocked from out of stock
        if (request.getQuantity() > 0) {
            publishInventoryEvent(saved, 0, InventoryEvent.INVENTORY_RESTOCKED);
        }
        
        log.info("Inventory added for product: {}, quantity: {}", saved.getProductId(), saved.getQuantity());
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
        
        Integer previousQuantity = inventory.getQuantity();
        inventory.setProductId(request.getProductId());
        inventory.setQuantity(request.getQuantity());
        Inventory updated = inventoryRepository.save(inventory);
        
        // Publish appropriate inventory events
        publishInventoryEvent(updated, previousQuantity, InventoryEvent.INVENTORY_UPDATED);
        checkAndPublishStockEvents(updated, previousQuantity);
        
        log.info("Inventory updated for product: {}, previous quantity: {}, new quantity: {}", 
                updated.getProductId(), previousQuantity, updated.getQuantity());
        return mapToResponse(updated);
    }

    public void deleteInventory(String id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + id));
        inventoryRepository.delete(inventory);
        log.info("Inventory deleted for product: {}", inventory.getProductId());
    }

    public void reduceInventory(String productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        
        Integer previousQuantity = inventory.getQuantity();
        if (previousQuantity < quantity) {
            throw new IllegalArgumentException("Insufficient inventory for product: " + productId + 
                    ". Available: " + previousQuantity + ", Requested: " + quantity);
        }
        
        inventory.setQuantity(previousQuantity - quantity);
        Inventory updated = inventoryRepository.save(inventory);
        
        // Publish inventory events
        publishInventoryEvent(updated, previousQuantity, InventoryEvent.INVENTORY_UPDATED);
        checkAndPublishStockEvents(updated, previousQuantity);
        
        log.info("Reduced inventory for product: {}, previous: {}, reduced by: {}, new: {}", 
                productId, previousQuantity, quantity, updated.getQuantity());
    }

    public void restoreInventory(String productId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        
        Integer previousQuantity = inventory.getQuantity();
        inventory.setQuantity(previousQuantity + quantity);
        Inventory updated = inventoryRepository.save(inventory);
        
        // Publish inventory events
        publishInventoryEvent(updated, previousQuantity, InventoryEvent.INVENTORY_UPDATED);
        
        // Check if restocked from out of stock or low stock
        if (previousQuantity == 0 && updated.getQuantity() > 0) {
            publishInventoryEvent(updated, previousQuantity, InventoryEvent.INVENTORY_RESTOCKED);
        }
        
        log.info("Restored inventory for product: {}, previous: {}, restored by: {}, new: {}", 
                productId, previousQuantity, quantity, updated.getQuantity());
    }

    private void checkAndPublishStockEvents(Inventory inventory, Integer previousQuantity) {
        Integer currentQuantity = inventory.getQuantity();
        
        // Check for out of stock
        if (currentQuantity == 0 && previousQuantity > 0) {
            publishInventoryEvent(inventory, previousQuantity, InventoryEvent.INVENTORY_OUT_OF_STOCK);
        }
        // Check for low stock
        else if (currentQuantity > 0 && currentQuantity <= LOW_STOCK_THRESHOLD && 
                 previousQuantity > LOW_STOCK_THRESHOLD) {
            publishInventoryEvent(inventory, previousQuantity, InventoryEvent.INVENTORY_LOW_STOCK);
        }
        // Check for restocking
        else if (previousQuantity == 0 && currentQuantity > 0) {
            publishInventoryEvent(inventory, previousQuantity, InventoryEvent.INVENTORY_RESTOCKED);
        }
    }

    private void publishInventoryEvent(Inventory inventory, Integer previousQuantity, String eventType) {
        try {
            InventoryEvent event = new InventoryEvent(
                    null, // eventId will be set by publisher
                    eventType,
                    inventory.getId(),
                    inventory.getProductId(),
                    inventory.getQuantity(),
                    previousQuantity,
                    LocalDateTime.now()
            );
            eventPublisherService.publishInventoryEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish inventory event: {} for product: {}", eventType, inventory.getProductId(), e);
        }
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .quantity(inventory.getQuantity())
                .build();
    }
}
