# Microservices E-Commerce Platform

A production-grade microservices architecture with:

- Spring Boot, Spring Cloud, MongoDB, Docker, Kafka
- JWT Auth & Role-Based Access (ADMIN, MANAGER, SUPPORT, USER)
- API Gateway, Eureka, Swagger, Feign
- Seeded dummy users for every role
- docker-compose for one-click startup

---

## Quick Start

1. Place all service folders at project root (`auth-service/`, `api-gateway/`, etc.)
2. From the project root, run:

   ```bash
   docker-compose up --build
