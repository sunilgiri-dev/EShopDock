package com.itransform.productservice.repository;

import com.itransform.productservice.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
    // Custom queries if needed
}
