# Kafka Integration Guide

## Overview

This microservices e-commerce platform now includes Apache Kafka for event-driven architecture, enabling real-time communication between services through asynchronous messaging.

## Architecture

### Event Flow
```
Order Service → Kafka Topics → Product/Inventory Services
Product Service → Kafka Topics → Order/Inventory Services  
Inventory Service → Kafka Topics → Order Service
```

### Kafka Topics

#### Order Events
- **order-created**: Published when a new order is created
- **order-updated**: Published when order status changes
- **order-cancelled**: Published when an order is cancelled
- **order-completed**: Published when an order is completed

#### Product Events
- **product-created**: Published when a new product is added
- **product-updated**: Published when product details are modified
- **product-deleted**: Published when a product is removed

#### Inventory Events
- **inventory-updated**: Published when inventory quantity changes
- **inventory-low-stock**: Published when inventory falls below threshold (10 items)
- **inventory-out-of-stock**: Published when inventory reaches zero
- **inventory-restocked**: Published when inventory is replenished

## Event Models

### OrderEvent
```json
{
  "eventId": "uuid",
  "eventType": "ORDER_CREATED|ORDER_UPDATED|ORDER_CANCELLED|ORDER_COMPLETED",
  "orderId": "order-id",
  "username": "user123",
  "items": [
    {
      "productId": "product-id",
      "quantity": 2,
      "price": 29.99
    }
  ],
  "totalAmount": 59.98,
  "status": "CREATED|PROCESSING|COMPLETED|CANCELLED",
  "timestamp": "2024-01-15T10:30:00"
}
```

### ProductEvent
```json
{
  "eventId": "uuid",
  "eventType": "PRODUCT_CREATED|PRODUCT_UPDATED|PRODUCT_DELETED",
  "productId": "product-id",
  "name": "Product Name",
  "description": "Product Description",
  "price": 29.99,
  "timestamp": "2024-01-15T10:30:00"
}
```

### InventoryEvent
```json
{
  "eventId": "uuid",
  "eventType": "INVENTORY_UPDATED|INVENTORY_LOW_STOCK|INVENTORY_OUT_OF_STOCK|INVENTORY_RESTOCKED",
  "inventoryId": "inventory-id",
  "productId": "product-id",
  "quantity": 50,
  "previousQuantity": 60,
  "timestamp": "2024-01-15T10:30:00"
}
```

## Service Integration

### Order Service
- **Publishes**: Order events on create, update, cancel operations
- **Consumes**: Product and inventory events for order processing validation

### Product Service
- **Publishes**: Product events on CRUD operations
- **Consumes**: Order events for analytics and product popularity tracking

### Inventory Service
- **Publishes**: Inventory events on quantity changes, stock alerts
- **Consumes**: Order events to automatically adjust inventory levels

### Auth Service
- **Publishes**: User authentication events (future enhancement)
- **Consumes**: None currently

## Configuration

### Kafka Properties
Each service includes Kafka configuration in `application.yml`:

```yaml
spring:
  kafka:
    bootstrap-servers: kafka:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
    consumer:
      group-id: {service-name}-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
        spring.json.use.type.info.headers: false
```

## Running the System

### Start Services
```bash
docker-compose up -d
```

This will start:
1. Zookeeper
2. Kafka
3. Kafka topic initializer
4. All microservices

### Verify Kafka Topics
```bash
docker exec -it kafka kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### Monitor Kafka Messages
```bash
# Monitor order events
docker exec -it kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order-created --from-beginning

# Monitor product events
docker exec -it kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic product-created --from-beginning

# Monitor inventory events
docker exec -it kafka kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic inventory-updated --from-beginning
```

## Testing Event Flow

### 1. Create a Product
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "role: ADMIN" \
  -d '{
    "name": "Test Product",
    "description": "A test product",
    "price": 29.99
  }'
```
**Expected**: `product-created` event published

### 2. Add Inventory
```bash
curl -X POST http://localhost:8080/api/v1/inventory \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "role: ADMIN" \
  -d '{
    "productId": "PRODUCT_ID_FROM_STEP_1",
    "quantity": 100
  }'
```
**Expected**: `inventory-updated` and `inventory-restocked` events published

### 3. Create an Order
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "username: testuser" \
  -H "role: USER" \
  -d '{
    "items": [
      {
        "productId": "PRODUCT_ID_FROM_STEP_1",
        "quantity": 5
      }
    ]
  }'
```
**Expected**: 
- `order-created` event published
- Inventory service reduces stock automatically
- `inventory-updated` event published

### 4. Cancel Order
```bash
curl -X DELETE http://localhost:8080/api/v1/orders/ORDER_ID \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "username: testuser" \
  -H "role: USER"
```
**Expected**:
- `order-cancelled` event published
- Inventory service restores stock automatically
- `inventory-updated` event published

## Error Handling

### Kafka Connection Issues
- Services will retry connection to Kafka with exponential backoff
- Failed events are logged but don't prevent main operations
- Manual intervention may be required for persistent issues

### Event Processing Failures
- Failed event processing is logged
- Messages are not acknowledged and will be reprocessed
- Dead letter queues can be implemented for permanent failures

## Best Practices

1. **Idempotency**: All event handlers should be idempotent
2. **Error Handling**: Implement proper error handling and logging
3. **Monitoring**: Monitor Kafka lag and processing times
4. **Schema Evolution**: Use compatible schema changes for events
5. **Testing**: Test event flows in integration tests

## Future Enhancements

1. **Saga Pattern**: Implement distributed transactions
2. **Event Sourcing**: Store events as source of truth
3. **CQRS**: Separate read and write models
4. **Dead Letter Queues**: Handle permanently failed messages
5. **Schema Registry**: Manage event schema evolution
6. **Metrics**: Add Kafka metrics and monitoring
7. **Authentication Events**: Publish user auth events
8. **Notification Service**: Email/SMS notifications via events

## Troubleshooting

### Common Issues

1. **Service startup order**: Ensure Kafka is ready before services start
2. **Topic creation**: Topics are auto-created or use the init script
3. **Serialization**: Ensure consistent serialization across services
4. **Consumer groups**: Use unique group IDs for different service instances

### Logs to Check
```bash
# Service logs
docker logs order-service
docker logs product-service
docker logs inventory-service

# Kafka logs
docker logs kafka
docker logs zookeeper
```

This Kafka integration provides a robust foundation for event-driven microservices communication and can be extended based on business requirements.