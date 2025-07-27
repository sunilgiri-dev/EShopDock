package com.itransform.inventoryservice.controller;

import com.itransform.inventoryservice.dto.*;
import com.itransform.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponse> addInventory(@Valid @RequestBody InventoryRequest request) {
        InventoryResponse created = inventoryService.addInventory(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAllInventory() {
        List<InventoryResponse> inventory = inventoryService.getAllInventory();
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable String productId) {
        InventoryResponse response = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable String id,
            @Valid @RequestBody InventoryRequest request
    ) {
        InventoryResponse updated = inventoryService.updateInventory(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventory(@PathVariable String id) {
        inventoryService.deleteInventory(id);
        return ResponseEntity.noContent().build();
    }
}
