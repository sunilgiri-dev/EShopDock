package com.learn.orderservice.repository;

import com.learn.orderservice.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;


import java.util.List;


public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByUsername(String username);
}
