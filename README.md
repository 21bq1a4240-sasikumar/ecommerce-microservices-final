# Ecommerce Microservices

A backend Ecommerce application built using **Java, Spring Boot, Spring Data JPA, PostgreSQL, and Docker**. The application follows a microservices architecture, with separate services for product catalog, inventory management, and order processing. An API Gateway provides a single entry point for client requests.

## 1. Architecture

The application consists of four services:

| Service | Port | Responsibility |
|---|---:|---|
| API Gateway | 8083 | Routes client requests to the appropriate service |
| Catalog Service | 8080 | Manages products and categories |
| Inventory Service | 8081 | Manages available and reserved stock |
| Order Service | 8082 | Processes orders and coordinates inventory reservations |

Each business service uses its own PostgreSQL database.

### Architecture flow

```text
                 Client
                   |
                   v
             API Gateway
               :8083
                   |
        +----------+----------+
        |          |          |
        v          v          v
     Catalog   Inventory    Order
     Service    Service    Service
      :8080      :8081      :8082
        |          |          |
        v          v          v
    Catalog DB Inventory DB  Order DB
```

## 2. Technologies Used

- Java
- Spring Boot
- Spring Data JPA
- Hibernate
- Spring Web / REST APIs
- Spring Cloud Gateway (MVC)
- PostgreSQL
- Maven
- Docker and Docker Compose
- WebClient
- Resilience4j
- Swagger / OpenAPI
- Postman
- Git and GitHub

## 3. Main Features

### Catalog Service
- Product management.
- Category management.
- Product retrieval and search.
- REST API integration.

### Inventory Service
- Stock retrieval.
- Add stock and release stock.
- Reserve stock during order processing.
- Available and reserved quantity tracking.

### Order Service
- Order placement.
- Product and inventory validation.
- Inventory reservation during order processing.
- Insufficient-stock handling.
- Idempotency-Key support to prevent duplicate order creation.
- Error handling for unavailable downstream services.
- Inventory rollback when an order cannot be completed.

### API Gateway
- Central entry point for API requests.
- Routes requests to the respective microservices.

## 4. API Endpoints

All the following requests are made through the API Gateway.

**Catalog Service**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/catalog/api/products` | Retrieve all products |

**Inventory Service**

| Method | Endpoint | Description |
|---|---|---|
| GET | `/inventory/api/inventory/{productId}` | Retrieve inventory |
| POST | `/inventory/api/inventory/{productId}/add?quantity=5` | Add stock |
| POST | `/inventory/api/inventory/{productId}/release?quantity=1` | Release stock |

**Order Service**

| Method | Endpoint | Description |
|---|---|---|
| POST | `/orders/api/orders` | Place an order |

Order placement requires an `Idempotency-Key` header.

### Sample Order Request

**URL**

`POST http://localhost:8083/orders/api/orders`

**Headers**

```text
Content-Type: application/json
Idempotency-Key: unique-order-key
```

**Request Body**

```json
{
  "items": [
    {
      "productId": 1,
      "quantity": 1
    }
  ]
}
```

## 5. Running the Application

### Prerequisites

- Docker Desktop
- Git (if cloning the repository)

### Step 1: Clone the repository

```bash
git clone <your-repository-url>
cd Ecommerce
```

### Step 2: Build and start all services

```bash
docker compose up -d --build
```

### Step 3: Check running containers

```bash
docker compose ps
```

The application includes four microservices and three PostgreSQL database containers.

### Step 4: View service logs

```bash
docker compose logs -f
```

To view an individual service:

```bash
docker compose logs -f order-service
```

### Step 5: Stop the application

```bash
docker compose down
```

To remove containers and their persistent database volumes:

```bash
docker compose down -v
```

**Warning:** The `-v` option deletes the database volumes and their stored data.

## 6. Testing

The application was manually tested using PowerShell REST requests and Postman.

The following scenarios were verified:

- Successful order creation.
- Inventory reservation after successful order placement.
- Repeated requests with the same idempotency key.
- Rejection of requests with missing or blank idempotency keys.
- Insufficient stock handling.
- Inventory rollback when an order fails.
- Invalid product and inventory handling.
- Invalid order quantities.
- Inventory service unavailability handling.

### Final End-to-End Test

A final order was successfully placed through the API Gateway.

| Field | Result |
|---|---|
| Order ID | 10 |
| Product ID | 1 |
| Quantity | 1 |
| Total Amount | 65,000 |
| Order Status | CONFIRMED |

Inventory before the order:

- Available quantity: 3
- Reserved quantity: 31

Inventory after the order:

- Available quantity: 2
- Reserved quantity: 32

This verified that successful order placement correctly updates inventory.

## 7. Error Responses

The application handles several business and validation errors:

| HTTP Status | Scenario |
|---|---|
| 400 | Invalid request or missing idempotency key |
| 404 | Product or inventory record not found |
| 409 | Insufficient stock |
| 503 | Inventory service unavailable |

## 8. Project Objective

The objective of this project was to understand and implement a distributed backend application using Spring Boot microservices.

It demonstrates REST API development, database integration, service-to-service communication, inventory reservation, order processing, idempotency, exception handling, and Docker-based deployment.