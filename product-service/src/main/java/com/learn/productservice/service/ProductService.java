package com.learn.productservice.service;

import com.learn.productservice.dto.*;
import com.learn.productservice.dto.ProductRequest;
import com.learn.productservice.dto.ProductResponse;
import com.learn.productservice.events.ProductEvent;
import com.learn.productservice.exception.ResourceNotFoundException;
import com.learn.productservice.model.Product;
import com.learn.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final EventPublisherService eventPublisherService;

    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .build();
        Product saved = productRepository.save(product);
        
        // Publish product created event
        publishProductCreatedEvent(saved);
        
        log.info("Product created successfully: {}", saved.getId());
        return mapToResponse(saved);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setPrice(request.getPrice());
        Product updated = productRepository.save(existing);
        
        // Publish product updated event
        publishProductUpdatedEvent(updated);
        
        log.info("Product updated successfully: {}", id);
        return mapToResponse(updated);
    }

    public void deleteProduct(String id) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        // Publish product deleted event before deletion
        publishProductDeletedEvent(existing);
        
        productRepository.delete(existing);
        log.info("Product deleted successfully: {}", id);
    }

    private void publishProductCreatedEvent(Product product) {
        try {
            log.info("Publishing product created event for product: {}", product.getId());
            ProductEvent event = createProductEvent(product, ProductEvent.PRODUCT_CREATED);
            eventPublisherService.publishProductEvent(event);
            log.info("Product created event published successfully for product: {}", product.getId());
        } catch (Exception e) {
            log.error("Failed to publish product created event for product: {}", product.getId(), e);
        }
    }

    private void publishProductUpdatedEvent(Product product) {
        try {
            ProductEvent event = createProductEvent(product, ProductEvent.PRODUCT_UPDATED);
            eventPublisherService.publishProductEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish product updated event for product: {}", product.getId(), e);
        }
    }

    private void publishProductDeletedEvent(Product product) {
        try {
            ProductEvent event = createProductEvent(product, ProductEvent.PRODUCT_DELETED);
            eventPublisherService.publishProductEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish product deleted event for product: {}", product.getId(), e);
        }
    }

    private ProductEvent createProductEvent(Product product, String eventType) {
        return new ProductEvent(
                null, // eventId will be set by publisher
                eventType,
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice().doubleValue(),
                LocalDateTime.now()
        );
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .build();
    }
}
