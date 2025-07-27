# E-Commerce Microservices API Documentation

## Table of Contents
1. [Getting Started](#getting-started)
2. [Architecture Overview](#architecture-overview)
3. [Authentication & Authorization](#authentication--authorization)
4. [API Endpoints](#api-endpoints)
5. [Error Handling](#error-handling)
6. [Testing Examples](#testing-examples)

---

## Getting Started

### Prerequisites
- Docker and Docker Compose installed
- At least 4GB RAM available for containers
- Ports 8080, 8761, 27017, 2181, 9092 available

### Starting the Application

1. **Clone and Navigate to Project**
   ```bash
   cd C:/e-shop-main/micro-services
   ```

2. **Start All Services**
   ```bash
   docker-compose up --build
   ```

3. **Wait for Services to Start**
   The application will be ready when you see:
   - MongoDB is healthy (port 27017)
   - Eureka Discovery Server is running (port 8761)
   - API Gateway is running (port 8080)
   - All microservices are registered with Eureka

4. **Verify Services**
   - Eureka Dashboard: http://localhost:8761
   - API Gateway Health: http://localhost:8080/actuator/health
   - Swagger UI (Auth Service): Available through service discovery

### Stopping the Application
```bash
docker-compose down
```

---

## Architecture Overview

### Services
- **API Gateway** (Port 8080): Entry point, JWT validation, routing
- **Discovery Server** (Port 8761): Eureka service registry
- **Auth Service**: User authentication and authorization
- **Product Service**: Product catalog management
- **Order Service**: Order processing and management
- **Inventory Service**: Stock management
- **MongoDB** (Port 27017): Database for all services
- **Kafka** (Port 9092): Event streaming (with Zookeeper on 2181)

### Service Communication
- All external requests go through API Gateway (port 8080)
- Internal service communication via Eureka service discovery
- JWT tokens for authentication and authorization

---

## Authentication & Authorization

### User Roles
- **ADMIN**: Full access to all operations
- **MANAGER**: Access to most operations except admin-only functions
- **SUPPORT**: Read access and limited write operations
- **USER**: Basic user operations (view products, manage own orders)

### Demo Users (Pre-seeded)
| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN |
| manager1 | manager123 | MANAGER |
| support1 | support123 | SUPPORT |
| user1 | user123 | USER |

### JWT Token
- Tokens expire after 12 hours
- Include role-based authorization
- Must be included in Authorization header: `Bearer <token>`

---

## API Endpoints

All requests go through the API Gateway at `http://localhost:8080`

### Authentication Service

#### POST /api/v1/auth/signup
Register a new user (USER role only)

**Request:**
```json
{
  "username": "newuser",
  "password": "password123",
  "role": "USER"
}
```

**Response:**
```json
"User registered"
```

#### POST /api/v1/auth/login
Authenticate user and get JWT token

**Request:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

#### POST /api/v1/auth/users/create
Create MANAGER or SUPPORT user (ADMIN only)

**Headers:**
```
Authorization: Bearer <admin_token>
```

**Request:**
```json
{
  "username": "newmanager",
  "password": "manager123",
  "role": "MANAGER"
}
```

**Response:**
```json
"User created by admin"
```

### Product Service

#### GET /api/v1/products
Get all products

**Response:**
```json
[
  {
    "id": "product123",
    "name": "Product Name",
    "description": "Product Description",
    "price": 29.99
  }
]
```

#### GET /api/v1/products/{id}
Get product by ID

**Response:**
```json
{
  "id": "product123",
  "name": "Product Name",
  "description": "Product Description",
  "price": 29.99
}
```

#### POST /api/v1/products
Create new product

**Headers:**
```
Authorization: Bearer <token>
```

**Request:**
```json
{
  "name": "New Product",
  "description": "Product Description",
  "price": 49.99
}
```

**Response:**
```json
{
  "id": "product456",
  "name": "New Product",
  "description": "Product Description",
  "price": 49.99
}
```

#### PUT /api/v1/products/{id}
Update product

**Headers:**
```
Authorization: Bearer <token>
```

**Request:**
```json
{
  "name": "Updated Product",
  "description": "Updated Description",
  "price": 59.99
}
```

#### DELETE /api/v1/products/{id}
Delete product

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** 204 No Content

### Order Service

#### POST /api/v1/orders
Create new order

**Headers:**
```
Authorization: Bearer <token>
```

**Request:**
```json
{
  "items": [
    {
      "productId": "product123",
      "quantity": 2
    },
    {
      "productId": "product456",
      "quantity": 1
    }
  ]
}
```

**Response:**
```json
{
  "id": "order789",
  "username": "user1",
  "items": [
    {
      "productId": "product123",
      "quantity": 2
    }
  ],
  "totalAmount": 59.98,
  "status": "PENDING",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

#### GET /api/v1/orders
Get orders (all for ADMIN/MANAGER/SUPPORT, own orders for USER)

**Headers:**
```
Authorization: Bearer <token>
```

**Response:**
```json
[
  {
    "id": "order789",
    "username": "user1",
    "items": [...],
    "totalAmount": 59.98,
    "status": "PENDING",
    "createdAt": "2024-01-15T10:30:00Z"
  }
]
```

#### GET /api/v1/orders/{id}
Get order by ID

**Headers:**
```
Authorization: Bearer <token>
```

**Response:**
```json
{
  "id": "order789",
  "username": "user1",
  "items": [...],
  "totalAmount": 59.98,
  "status": "PENDING",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### Inventory Service

#### GET /api/v1/inventory
Get all inventory items

**Response:**
```json
[
  {
    "id": "inv123",
    "productId": "product123",
    "quantity": 100
  }
]
```

#### GET /api/v1/inventory/{productId}
Get inventory for specific product

**Response:**
```json
{
  "id": "inv123",
  "productId": "product123",
  "quantity": 100
}
```

#### POST /api/v1/inventory
Add inventory item

**Headers:**
```
Authorization: Bearer <token>
```

**Request:**
```json
{
  "productId": "product123",
  "quantity": 50
}
```

**Response:**
```json
{
  "id": "inv456",
  "productId": "product123",
  "quantity": 50
}
```

#### PUT /api/v1/inventory/{id}
Update inventory

**Headers:**
```
Authorization: Bearer <token>
```

**Request:**
```json
{
  "productId": "product123",
  "quantity": 75
}
```

#### DELETE /api/v1/inventory/{id}
Delete inventory item

**Headers:**
```
Authorization: Bearer <token>
```

**Response:** 204 No Content

---

## Error Handling

### Common HTTP Status Codes
- **200 OK**: Successful GET request
- **201 Created**: Successful POST request
- **204 No Content**: Successful DELETE request
- **400 Bad Request**: Invalid request data
- **401 Unauthorized**: Missing or invalid token
- **403 Forbidden**: Insufficient permissions
- **404 Not Found**: Resource not found
- **500 Internal Server Error**: Server error

### Error Response Format
```json
{
  "error": "Error message",
  "timestamp": "2024-01-15T10:30:00Z",
  "path": "/api/v1/products/123"
}
```

---

## Testing Examples

### 1. Complete User Journey

#### Step 1: Login as Admin
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

#### Step 2: Create a Product
```bash
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin_token>" \
  -d '{
    "name": "Test Product",
    "description": "A test product",
    "price": 29.99
  }'
```

#### Step 3: Add Inventory
```bash
curl -X POST http://localhost:8080/api/v1/inventory \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin_token>" \
  -d '{
    "productId": "<product_id>",
    "quantity": 100
  }'
```

#### Step 4: Login as User
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user1", "password": "user123"}'
```

#### Step 5: Create Order
```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <user_token>" \
  -d '{
    "items": [
      {
        "productId": "<product_id>",
        "quantity": 2
      }
    ]
  }'
```

### 2. Testing with Postman

1. **Import Collection**: Create a Postman collection with the endpoints above
2. **Set Environment Variables**: 
   - `base_url`: http://localhost:8080
   - `admin_token`: (obtained from login)
   - `user_token`: (obtained from login)
3. **Use Pre-request Scripts**: Automatically set authorization headers

### 3. Testing Role-Based Access

#### Test USER Role Restrictions
```bash
# This should fail (403 Forbidden)
curl -X POST http://localhost:8080/api/v1/auth/users/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <user_token>" \
  -d '{
    "username": "testmanager",
    "password": "password123",
    "role": "MANAGER"
  }'
```

#### Test ADMIN Privileges
```bash
# This should succeed
curl -X POST http://localhost:8080/api/v1/auth/users/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin_token>" \
  -d '{
    "username": "testmanager",
    "password": "password123",
    "role": "MANAGER"
  }'
```

---

## Additional Notes

### Service Discovery
- All services register with Eureka at startup
- API Gateway uses service discovery for load balancing
- Access Eureka dashboard at http://localhost:8761

### Database Access
- MongoDB runs in Docker container on port 27017
- Each service has its own database
- Internal connection string: mongodb://mongo:27017
- External connection string (from host): mongodb://localhost:27017

### Health Checks
- Each service exposes health endpoints
- Docker Compose includes health checks
- Services wait for dependencies to be healthy

### Swagger Documentation
- Each service may have Swagger UI enabled
- Access through service discovery or direct service ports

### Security Notes
- JWT secret is configurable via environment variable
- Passwords are BCrypt encrypted
- CORS is configured in API Gateway
- Rate limiting can be added to API Gateway

### Performance Considerations
- Each service has resource limits in Docker
- MongoDB has connection pooling
- Kafka is available for event-driven communication
- Consider implementing circuit breakers for production

---

## Troubleshooting

### Common Issues

#### 1. **Service Discovery Connection Error**
**Error**: `No such host is known (discovery-server)`

**Solutions**:
```bash
# Step 1: Check all services status
docker-compose ps

# Step 2: Stop and clean up
docker-compose down --remove-orphans

# Step 3: Start services in stages
docker-compose up mongo
# Wait for MongoDB to be ready (check logs)

docker-compose up discovery-server
# Wait for Discovery Server to be ready (check logs)

# Step 4: Check Discovery Server health
curl http://localhost:8761/actuator/health

# Step 5: Start remaining services
docker-compose up

# Alternative: Force recreate
docker-compose up --build --force-recreate
```

#### 2. **Services not starting**: Check Docker logs
```bash
# Check all services
docker-compose logs

# Check specific service
docker-compose logs <service-name>

# Follow logs in real-time
docker-compose logs -f <service-name>
```

#### 3. **Port conflicts**: Ensure ports 8080, 8761, 27017 are available
```bash
# Check what's using the ports
netstat -an | findstr "8080 8761 27017"

# Kill processes if needed
taskkill /F /PID <process-id>
```

#### 4. **Docker Network Issues**
```bash
# Check Docker networks
docker network ls

# Inspect the compose network
docker network inspect micro-services_default

# Restart Docker if needed
```

#### 5. **Health Check Failures**
```bash
# Check health status
docker-compose ps

# If mongo health check fails
docker exec -it mongo mongo --eval "db.adminCommand('ping')"

# If discovery-server health check fails
docker exec -it discovery-server curl -f http://localhost:8761/actuator/health
```

#### 6. **Authentication issues**: Verify JWT token format and expiration

#### 7. **Database connection issues**: Ensure MongoDB is running and accessible

### Step-by-Step Debugging

#### If Services Won't Start:
1. **Clean Environment**:
   ```bash
   docker-compose down -v
   docker system prune -f
   ```

2. **Start Services One by One**:
   ```bash
   # Start MongoDB first
   docker-compose up mongo
   
   # Verify MongoDB is ready
   docker-compose logs mongo
   
   # Start Discovery Server
   docker-compose up discovery-server
   
   # Verify Discovery Server is ready
   curl http://localhost:8761
   
   # Start remaining services
   docker-compose up
   ```

3. **Check Service Registration**:
   - Open http://localhost:8761 in browser
   - Verify all services are registered

#### If API Gateway Can't Connect:
1. **Check Network Connectivity**:
   ```bash
   # Test from API Gateway container
   docker exec -it api-gateway ping discovery-server
   
   # Test DNS resolution
   docker exec -it api-gateway nslookup discovery-server
   ```

2. **Verify Service URLs**:
   - Check application.yml files for correct service URLs
   - Ensure Eureka URLs use container names

### Monitoring & Health Checks

#### Service Health Endpoints:
- API Gateway: http://localhost:8080/actuator/health
- Discovery Server: http://localhost:8761/actuator/health
- Eureka Dashboard: http://localhost:8761

#### Monitoring Commands:
```bash
# Monitor resource usage
docker stats

# Check service logs
docker-compose logs -f

# Monitor specific service
docker-compose logs -f api-gateway

# Check container processes
docker-compose top
```

### Production Considerations

#### For Production Deployment:
1. **Use External Configuration**: Environment-specific configs
2. **Health Checks**: Implement comprehensive health checks
3. **Monitoring**: Use APM tools like Micrometer, Prometheus
4. **Logging**: Centralized logging with ELK stack
5. **Security**: Use proper secrets management
6. **Scaling**: Configure auto-scaling policies

#### Performance Optimization:
- Increase JVM heap size if needed
- Configure connection pools
- Implement circuit breakers
- Use caching where appropriate