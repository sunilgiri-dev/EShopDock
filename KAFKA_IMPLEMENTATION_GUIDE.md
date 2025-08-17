# Kafka Integration Implementation Guide

## Table of Contents
1. [Overview](#overview)
2. [Changes Made](#changes-made)
3. [Starting the Application](#starting-the-application)
4. [API Testing Guide](#api-testing-guide)
5. [Kafka Monitoring](#kafka-monitoring)
6. [Event Flow Testing](#event-flow-testing)
7. [Troubleshooting](#troubleshooting)

## Overview

This document outlines the complete Kafka integration implementation for the e-commerce microservices platform. Kafka enables event-driven architecture with asynchronous communication between services.

### Architecture Changes
- **Before**: Direct service-to-service communication
- **After**: Event-driven communication through Kafka topics
- **Benefits**: Loose coupling, scalability, resilience, real-time processing

## Changes Made

### 1. Dependencies Added

#### Parent POM (`pom.xml`)
```xml
<!-- Added Spring Kafka dependency management -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

#### Service-Level Changes
**Services Updated**: `auth-service`, `order-service`, `product-service`, `inventory-service`

**Added to each service's `pom.xml`:**
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### 2. Event Models Created

#### Order Service Events
**Location**: `order-service/src/main/java/com/itransform/orderservice/events/`

- `OrderEvent.java` - Order lifecycle events
- `ProductEvent.java` - Product information for order processing
- `InventoryEvent.java` - Inventory status for order validation

#### Product Service Events
**Location**: `product-service/src/main/java/com/itransform/productservice/events/`

- `ProductEvent.java` - Product CRUD events
- `OrderEvent.java` - Order information for analytics

#### Inventory Service Events
**Location**: `inventory-service/src/main/java/com/itransform/inventoryservice/events/`

- `InventoryEvent.java` - Inventory change events
- `OrderEvent.java` - Order information for stock management

### 3. Kafka Configuration Classes

#### Each Service Contains:
**File**: `config/KafkaConfig.java`

**Features**:
- Producer configuration with idempotency
- Consumer configuration with JSON deserialization
- Acknowledgment-based message processing
- Error handling and retry mechanisms

**Key Configuration**:
```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      acks: all
      retries: 3
      enable.idempotence: true
    consumer:
      group-id: {service-name}-group
      auto-offset-reset: earliest
```

### 4. Event Publishers

#### Service-Level Publishers
**File**: `service/EventPublisherService.java`

**Features**:
- Asynchronous event publishing
- Topic routing based on event type
- Error handling and logging
- UUID generation for event IDs

### 5. Event Listeners

#### Cross-Service Communication
- **Order Service** ← Listens to → Product & Inventory events
- **Product Service** ← Listens to → Order events
- **Inventory Service** ← Listens to → Order events

**Features**:
- Manual acknowledgment for reliability
- Error handling without acknowledgment
- Idempotent event processing

### 6. Business Logic Integration

#### Order Service (`OrderService.java`)
**Changes**:
- Event publishing on order creation
- Event publishing on order updates/cancellation
- Added `updateOrderStatus()` and `cancelOrder()` methods

#### Product Service (`ProductService.java`)
**Changes**:
- Event publishing on product CRUD operations
- Analytics tracking from order events

#### Inventory Service (`InventoryService.java`)
**Changes**:
- Automatic inventory adjustment based on order events
- Stock alert events (low stock, out of stock, restocked)
- Added `reduceInventory()` and `restoreInventory()` methods

### 7. REST API Enhancements

#### Order Controller (`OrderController.java`)
**New Endpoints**:
```java
PUT /api/v1/orders/{id}/status - Update order status
DELETE /api/v1/orders/{id} - Cancel order
```

### 8. Infrastructure Changes

#### Docker Compose (`docker-compose.yml`)
**Added**:
- Zookeeper service
- Kafka broker with health checks
- Kafka topic initializer
- Service dependencies on Kafka

#### Kafka Topic Initialization (`init-kafka-topics.sh`)
**Topics Created**:
- Order topics: `order-created`, `order-updated`, `order-cancelled`, `order-completed`
- Product topics: `product-created`, `product-updated`, `product-deleted`
- Inventory topics: `inventory-updated`, `inventory-low-stock`, `inventory-out-of-stock`, `inventory-restocked`

## Starting the Application

### Prerequisites
- Docker and Docker Compose installed
- Java 17 (if running locally)
- Maven (if building locally)

### Step 1: Clean Build (Optional)
```bash
# Navigate to the project root
cd C:/Users/sunil/Downloads/e-shop-main/e-shop-main/micro-services

# Clean and build all services
mvn clean install
```

### Step 2: Start Infrastructure and Services
```bash
# Start all services (this will also build images)
docker-compose up --build -d

# Or start without building (if images exist)
docker-compose up -d
```

### Step 3: Verify Services are Running
```bash
# Check all containers
docker ps

# Expected containers:
# - mongo
# - zookeeper  
# - kafka
# - kafka-init (will exit after topic creation)
# - discovery-server
# - api-gateway
# - auth-service
# - product-service
# - order-service
# - inventory-service
```

### Step 4: Wait for Services to be Ready
```bash
# Check service health (retry if needed)
curl http://localhost:8080/actuator/health

# Check Eureka dashboard
http://localhost:8761
```

### Step 5: Verify Kafka Topics
```bash
# List all Kafka topics
docker exec -it kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# Expected topics:
# order-created, order-updated, order-cancelled, order-completed
# product-created, product-updated, product-deleted  
# inventory-updated, inventory-low-stock, inventory-out-of-stock, inventory-restocked
```

## API Testing Guide

### Step 1: Get Authentication Token

#### Register a User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "role": "USER"
  }'
```

#### Register an Admin
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123", 
    "role": "ADMIN"
  }'
```

#### Login to Get JWT Token
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Save the JWT token from response for subsequent requests.**

### Step 2: Test Product Service

#### Create a Product
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "role: ADMIN" \
  -d '{
    "name": "Laptop",
    "description": "Gaming Laptop",
    "price": 1299.99
  }'
```

**Expected Events**: `product-created` event published to Kafka

#### Get All Products
```bash
curl -X GET http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "role: ADMIN"
```

#### Update a Product
```bash
curl -X PUT http://localhost:8080/api/v1/products/PRODUCT_ID \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "role: ADMIN" \
  -d '{
    "name": "Gaming Laptop Pro",
    "description": "High-end Gaming Laptop", 
    "price": 1599.99
  }'
```

**Expected Events**: `product-updated` event published to Kafka

### Step 3: Test Inventory Service

#### Add Inventory
```bash
curl -X POST http://localhost:8080/api/v1/inventory \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "role: ADMIN" \
  -d '{
    "productId": "PRODUCT_ID_FROM_PREVIOUS_STEP",
    "quantity": 50
  }'
```

**Expected Events**: `inventory-updated` and `inventory-restocked` events published

#### Get Inventory by Product ID
```bash
curl -X GET http://localhost:8080/api/v1/inventory/product/PRODUCT_ID \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "role: ADMIN"
```

#### Update Inventory
```bash
curl -X PUT http://localhost:8080/api/v1/inventory/INVENTORY_ID \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "role: ADMIN" \
  -d '{
    "productId": "PRODUCT_ID",
    "quantity": 5
  }'
```

**Expected Events**: `inventory-updated` and `inventory-low-stock` events (if quantity ≤ 10)

### Step 4: Test Order Service

#### Create an Order
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "username: testuser" \
  -H "role: USER" \
  -d '{
    "items": [
      {
        "productId": "PRODUCT_ID",
        "quantity": 2
      }
    ]
  }'
```

**Expected Events**: 
- `order-created` event published
- Inventory service automatically reduces stock
- `inventory-updated` event published

#### Get Orders
```bash
# Get user's orders
curl -X GET http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "username: testuser" \
  -H "role: USER"

# Get all orders (admin only)
curl -X GET http://localhost:8080/api/v1/orders \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "username: admin" \
  -H "role: ADMIN"
```

#### Update Order Status
```bash
curl -X PUT http://localhost:8080/api/v1/orders/ORDER_ID/status?status=PROCESSING \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "role: ADMIN"
```

**Expected Events**: `order-updated` event published

#### Cancel an Order
```bash
curl -X DELETE http://localhost:8080/api/v1/orders/ORDER_ID \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "username: testuser" \
  -H "role: USER"
```

**Expected Events**:
- `order-cancelled` event published
- Inventory service automatically restores stock
- `inventory-updated` event published

## Kafka Monitoring

### Monitor All Topics
```bash
# List all topics
docker exec -it kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### Monitor Specific Topic Messages
```bash
# Monitor order events
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-created \
  --from-beginning

# Monitor product events  
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic product-created \
  --from-beginning

# Monitor inventory events
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic inventory-updated \
  --from-beginning
```

### Check Topic Details
```bash
# Describe a topic
docker exec -it kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --topic order-created \
  --describe

# Check consumer group status
docker exec -it kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --list
```

### Monitor Consumer Lag
```bash
# Check consumer group details
docker exec -it kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group order-service-group \
  --describe
```

## Event Flow Testing

### Complete End-to-End Test Scenario

#### 1. Create Product and Inventory
```bash
# Create product
PRODUCT_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "role: ADMIN" \
  -d '{
    "name": "Test Product",
    "description": "A test product",
    "price": 29.99
  }')

# Extract product ID
PRODUCT_ID=$(echo $PRODUCT_RESPONSE | jq -r '.id')

# Add inventory
curl -X POST http://localhost:8080/api/v1/inventory \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "role: ADMIN" \
  -d "{
    \"productId\": \"$PRODUCT_ID\",
    \"quantity\": 100
  }"
```

#### 2. Monitor Events in Real-Time
```bash
# In separate terminals, monitor different topics:

# Terminal 1: Order events
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-created

# Terminal 2: Inventory events
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic inventory-updated

# Terminal 3: Product events
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic product-created
```

#### 3. Create Order and Watch Events
```bash
# Create an order
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "username: testuser" \
  -H "role: USER" \
  -d "{
    \"items\": [
      {
        \"productId\": \"$PRODUCT_ID\",
        \"quantity\": 5
      }
    ]
  }")

ORDER_ID=$(echo $ORDER_RESPONSE | jq -r '.id')
```

**You should see**:
1. `order-created` event in Terminal 1
2. `inventory-updated` event in Terminal 2 (stock reduced from 100 to 95)

#### 4. Cancel Order and Watch Events
```bash
# Cancel the order
curl -X DELETE http://localhost:8080/api/v1/orders/$ORDER_ID \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "username: testuser" \
  -H "role: USER"
```

**You should see**:
1. `order-cancelled` event in Terminal 1
2. `inventory-updated` event in Terminal 2 (stock restored from 95 to 100)

## Troubleshooting

### Common Issues

#### 1. Services Not Starting
```bash
# Check service logs
docker logs auth-service
docker logs product-service
docker logs order-service
docker logs inventory-service

# Check if all dependencies are running
docker ps | grep -E "(mongo|kafka|zookeeper|discovery-server)"
```

#### 2. Kafka Connection Issues
```bash
# Check Kafka logs
docker logs kafka
docker logs zookeeper

# Verify Kafka is accessible
docker exec -it kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# Restart Kafka if needed
docker restart kafka
```

#### 3. Authentication Issues
```bash
# Verify JWT token is valid
# The token should be in format: eyJ...

# Check auth service logs
docker logs auth-service

# Try registering a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser2", "password": "password123", "role": "USER"}'
```

#### 4. Events Not Being Published/Consumed
```bash
# Check service logs for Kafka connection errors
docker logs order-service | grep -i kafka
docker logs product-service | grep -i kafka
docker logs inventory-service | grep -i kafka

# Verify topics exist
docker exec -it kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# Check consumer groups
docker exec -it kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

#### 5. Database Connection Issues
```bash
# Check MongoDB logs
docker logs mongo

# Verify databases are created
docker exec -it mongo mongosh --eval "show dbs"
```

### Logs to Monitor
```bash
# Real-time log monitoring
docker logs -f order-service
docker logs -f product-service  
docker logs -f inventory-service
docker logs -f kafka
```

### Health Checks
```bash
# Check application health
curl http://localhost:8080/actuator/health

# Check individual service health via Eureka
curl http://localhost:8761/eureka/apps

# Check Swagger documentation
http://localhost:8080/swagger-ui.html
```

## Performance Testing

### Load Testing with Multiple Orders
```bash
# Create multiple orders quickly to test event processing
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/v1/orders \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer YOUR_JWT_TOKEN" \
    -H "username: testuser" \
    -H "role: USER" \
    -d "{
      \"items\": [
        {
          \"productId\": \"$PRODUCT_ID\",
          \"quantity\": 1
        }
      ]
    }" &
done
```

### Monitor Consumer Lag During Load
```bash
# Monitor consumer groups during load testing
watch "docker exec -it kafka kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group inventory-service-order-group --describe"
```

This completes the comprehensive guide for Kafka integration implementation and testing. The system now provides robust event-driven communication between all microservices with proper error handling and monitoring capabilities.