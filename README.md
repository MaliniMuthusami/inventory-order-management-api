# Inventory & Order Management API

A secure, production-ready REST API built with **Java 17** and **Spring Boot 3.2** for managing products, categories, inventory levels, and customer orders. Features JWT-based authentication, role-based access control, and automatic stock management on order placement and cancellation.

---

## Table of Contents
- [Tech Stack](#tech-stack)
- [Features](#features)
- [Project Structure](#project-structure)
- [Setup & Run](#setup--run)
- [Authentication](#authentication)
- [API Endpoints](#api-endpoints)
- [Swagger UI](#swagger-ui)
- [Order Lifecycle](#order-lifecycle)
- [Running Tests](#running-tests)
- [Postman Collection](#postman-collection)
- [Environment Variables](#environment-variables)

---

## Tech Stack

| Layer       | Technology                                    |
|-------------|-----------------------------------------------|
| Language    | Java 17                                       |
| Framework   | Spring Boot 3.2                               |
| Security    | Spring Security + JWT (JJWT 0.11) + BCrypt    |
| Database    | MongoDB (Spring Data MongoDB)                 |
| API Docs    | SpringDoc OpenAPI 3 / Swagger UI              |
| Build       | Maven                                         |
| Testing     | JUnit 5, MockMvc, Flapdoodle Embedded MongoDB |

---

## Features

- **JWT Authentication** — stateless token-based auth, 24-hour expiry
- **BCrypt Password Encryption** — passwords hashed at registration
- **Role-Based Access Control** — `ROLE_ADMIN` and `ROLE_USER` with endpoint-level enforcement
- **Category Management** — full CRUD, many-to-many with products
- **Product Management** — create, update, activate/deactivate, paginate, sort, search by name, filter by category, view low-stock
- **Order Management** — place orders with automatic stock validation and deduction, confirm, cancel with stock restoration
- **Pagination** — all list endpoints return page metadata (`totalElements`, `totalPages`, etc.)
- **Custom Queries** — MongoDB `@Query` annotations for case-insensitive search, category filter, and low-stock threshold
- **Global Exception Handling** — consistent JSON error responses for all failure cases
- **Swagger UI** — full interactive API documentation at `/swagger-ui.html`

---

## Project Structure

```
src/main/java/com/inventory/
├── config/
│   ├── SecurityConfig.java          # Role-based access rules, JWT filter wiring
│   └── SwaggerConfig.java           # OpenAPI 3 + Bearer auth setup
├── controller/
│   ├── AuthController.java          # POST /api/auth/register, /login
│   ├── CategoryController.java      # CRUD /api/categories
│   ├── ProductController.java       # CRUD + search/filter /api/products
│   └── OrderController.java         # Place/confirm/cancel /api/orders
├── document/                        # MongoDB documents
│   ├── User.java                    # Implements UserDetails
│   ├── Role.java                    # Enum: ROLE_ADMIN, ROLE_USER
│   ├── Category.java
│   ├── Product.java                 # price stored as Double for sortability
│   ├── Order.java
│   ├── OrderItem.java               # Embedded — price snapshot at order time
│   └── OrderStatus.java             # Enum: CREATED, CONFIRMED, CANCELLED
├── dto/
│   ├── request/                     # AuthRequest, CategoryRequest, ProductRequest, OrderRequest
│   └── response/                    # ApiResponse (all DTOs), PageResponse<T>
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   ├── DuplicateResourceException.java
│   ├── BadRequestException.java
│   └── InsufficientStockException.java
├── repository/
│   ├── UserRepository.java
│   ├── CategoryRepository.java
│   ├── ProductRepository.java       # Custom @Query: search, category filter, low-stock
│   └── OrderRepository.java
├── security/
│   ├── JwtUtils.java                # Token generation and validation
│   ├── JwtAuthenticationFilter.java # Extracts + validates token per request
│   └── UserDetailsServiceImpl.java
└── service/impl/
    ├── AuthServiceImpl.java
    ├── CategoryServiceImpl.java
    ├── ProductServiceImpl.java
    └── OrderServiceImpl.java        # Stock deduction + restore logic
```

### MongoDB Collections

| Collection   | Description                                                         |
|--------------|---------------------------------------------------------------------|
| `users`      | Registered users — BCrypt-hashed passwords, role field             |
| `categories` | Product categories — unique by name                                |
| `products`   | Products — category IDs stored as reference list (Many-to-Many)    |
| `orders`     | Orders — `OrderItem` embedded as array (price snapshot preserved)  |

---

## Setup & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- MongoDB (local or Docker)

### Start MongoDB with Docker
```bash
docker run -d -p 27017:27017 --name mongo mongo:7
```

### Clone and build
```bash
git clone <repo-url>
cd inventory-api
mvn clean package -DskipTests
```

### Run
```bash
mvn spring-boot:run
```

The API starts at **`http://localhost:8080`**

---

## Authentication

All endpoints except `/api/auth/**` require a JWT token in the `Authorization` header.

### Step 1 — Register

```http
POST http://localhost:8080/api/auth/register?role=ADMIN
Content-Type: application/json

{
  "username": "john_admin",
  "email": "john@example.com",
  "password": "secret123"
}
```

```http
POST http://localhost:8080/api/auth/register?role=USER
Content-Type: application/json

{
  "username": "jane_user",
  "email": "jane@example.com",
  "password": "secret123"
}
```

### Step 2 — Login

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "john_admin",
  "password": "secret123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "username": "john_admin",
  "email": "john@example.com",
  "role": "ROLE_ADMIN"
}
```

### Step 3 — Use the token

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

## API Endpoints

### Role Access Matrix

| Endpoint Group              | ROLE_ADMIN | ROLE_USER |
|-----------------------------|------------|-----------|
| `POST /api/auth/**`         | ✅          | ✅        |
| `GET /api/products/**`      | ✅          | ✅        |
| `POST/PUT/DELETE /api/products/**` | ✅  | ❌        |
| `ALL /api/categories/**`    | ✅          | ❌        |
| `ALL /api/orders/**`        | ❌          | ✅        |

---

### Authentication

| Method | Endpoint                        | Auth | Description                     |
|--------|---------------------------------|------|---------------------------------|
| POST   | `/api/auth/register?role=ADMIN` | ❌   | Register an admin user          |
| POST   | `/api/auth/register?role=USER`  | ❌   | Register a regular user         |
| POST   | `/api/auth/login`               | ❌   | Login and receive JWT token     |

---

### Categories — `[ADMIN only]`

| Method | Endpoint                | Description              |
|--------|-------------------------|--------------------------|
| POST   | `/api/categories`       | Create a new category    |
| GET    | `/api/categories`       | Get all categories       |
| GET    | `/api/categories/{id}`  | Get category by ID       |
| PUT    | `/api/categories/{id}`  | Update a category        |
| DELETE | `/api/categories/{id}`  | Delete a category        |

**Create Category Request:**
```json
{
  "name": "Electronics",
  "description": "Electronic devices and accessories"
}
```

---

### Products — `[ADMIN write | ALL read]`

| Method | Endpoint                                                       | Role  | Description                          |
|--------|----------------------------------------------------------------|-------|--------------------------------------|
| POST   | `/api/products`                                               | ADMIN | Create a product                     |
| GET    | `/api/products?page=0&size=10&sortBy=price&direction=asc`     | ALL   | List active products (paginated)     |
| GET    | `/api/products/{id}`                                          | ALL   | Get product by ID                    |
| PUT    | `/api/products/{id}`                                          | ADMIN | Update product (partial update)      |
| DELETE | `/api/products/{id}`                                          | ADMIN | Delete a product                     |
| GET    | `/api/products/search?name=wireless`                          | ALL   | Search by name (case-insensitive)    |
| GET    | `/api/products/category/{categoryId}?sortBy=price&direction=asc` | ALL | Filter by category with sort    |
| GET    | `/api/products/low-stock?threshold=10`                        | ALL   | Products at or below stock threshold |

**Sort fields:** `name`, `price`, `stockQuantity`, `createdAt`

**Create Product Request:**
```json
{
  "name": "Wireless Headphones",
  "description": "Noise-cancelling over-ear headphones",
  "price": 149.99,
  "stockQuantity": 50,
  "categoryIds": ["<categoryId>"]
}
```

**Paginated Response:**
```json
{
  "content": [...],
  "page": 0,
  "size": 10,
  "totalElements": 25,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

---

### Orders — `[USER only]`

| Method | Endpoint                     | Description                                        |
|--------|------------------------------|----------------------------------------------------|
| POST   | `/api/orders`                | Place a new order (validates + deducts stock)      |
| GET    | `/api/orders?status=CREATED` | View own order history (paginated, filterable)     |
| GET    | `/api/orders/{id}`           | Get a specific order                               |
| PATCH  | `/api/orders/{id}/confirm`   | Confirm a CREATED order                            |
| PATCH  | `/api/orders/{id}/cancel`    | Cancel order (automatically restores stock)        |

**Place Order Request:**
```json
{
  "items": [
    { "productId": "<productId>", "quantity": 3 },
    { "productId": "<productId2>", "quantity": 1 }
  ]
}
```

**Order Response:**
```json
{
  "id": "64xyz789abc123",
  "username": "jane_user",
  "items": [
    {
      "productId": "64abc123def456",
      "productName": "Wireless Headphones",
      "quantity": 3,
      "unitPrice": 149.99,
      "subtotal": 449.97
    }
  ],
  "totalAmount": 449.97,
  "status": "CREATED",
  "createdAt": "2026-04-12T18:30:00"
}
```

---

## Swagger UI

Once the application is running:

| URL | Description |
|-----|-------------|
| **`http://localhost:8080/swagger-ui.html`** | Interactive Swagger UI |
| `http://localhost:8080/api-docs` | Raw OpenAPI JSON |
| `http://localhost:8080/api-docs.yaml` | Raw OpenAPI YAML |

**How to use Swagger UI:**
1. Open `http://localhost:8080/swagger-ui.html`
2. Use `POST /api/auth/register?role=ADMIN` to create an admin account
3. Use `POST /api/auth/login` to get your token
4. Click the **Authorize 🔒** button at the top right
5. Enter your token (just the token string, no "Bearer" prefix needed in the UI)
6. All secured endpoints are now accessible

The `openapi.yaml` file in the project root is a standalone specification that can be imported into Swagger Editor, Postman, or any API portal.

---

## Order Lifecycle

```
Place Order
    │
    ▼
 CREATED ──────► CONFIRMED
    │
    └──────────► CANCELLED
                 (stock restored)
```

**Stock Management Rules:**

| Action | Stock Effect |
|--------|--------------|
| Place order | Stock is **validated** for all items first, then **deducted** atomically |
| Confirm order | No stock change — confirms intent to purchase |
| Cancel CREATED order | Stock is **restored** for every item in the order |
| Cancel CONFIRMED order | ❌ Not allowed — returns `400 Bad Request` |
| Order inactive product | ❌ Not allowed — returns `400 Bad Request` |
| Insufficient stock | ❌ Returns `409 Conflict` with details of which product and how much is available |

---

## Running Tests

Tests use **Flapdoodle Embedded MongoDB** — no external MongoDB instance needed.

```bash
mvn test
```

| Test Class                | Cases | Coverage                                                               |
|---------------------------|-------|------------------------------------------------------------------------|
| `AuthControllerTest`      | 8     | Register (ADMIN/USER), login, duplicate username/email, validation     |
| `CategoryControllerTest`  | 9     | Full CRUD, admin-only enforcement (USER → 403), duplicate name         |
| `ProductControllerTest`   | 12    | Create, update, delete, search, filter, sort desc, low-stock, role     |
| `OrderControllerTest`     | 13    | Place order, stock deduction, confirm, cancel + restore, isolation     |
| `OrderServiceTest`        | 10    | Unit: stock logic, inactive product, insufficient stock, cancel restore |

---

## Postman Collection

Import **`InventoryAPI.postman_collection.json`** into Postman.

Requests auto-save tokens and IDs between steps using test scripts. Run in this order:

1. **Register Admin** → `adminToken` variable saved automatically
2. **Register User** → `userToken` variable saved automatically
3. **Create Category** → `categoryId` saved
4. **Create Product** → `productId` saved
5. **Place Order** → `orderId` saved
6. **Confirm / Cancel Order**

---

## Error Responses

All errors return a consistent JSON structure:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: abc123",
  "timestamp": "2026-04-12T18:30:00"
}
```

Validation errors include a `fieldErrors` map:

```json
{
  "status": 400,
  "error": "Validation Failed",
  "fieldErrors": {
    "name": "Product name is required",
    "price": "Price must be greater than 0"
  },
  "timestamp": "2026-04-12T18:30:00"
}
```

---

## Environment Variables

| Variable           | Default Value                             | Description                        |
|--------------------|-------------------------------------------|------------------------------------|
| `MONGODB_URI`      | `mongodb://localhost:27017/inventory_db`  | Full MongoDB connection string     |
| `MONGODB_DATABASE` | `inventory_db`                            | Database name                      |
| `JWT_SECRET`       | Built-in 64-char hex key                  | HS256 signing secret — **change in production** |

Set via environment before running:
```bash
export MONGODB_URI=mongodb://localhost:27017/inventory_db
export MONGODB_DATABASE=inventory_db
export JWT_SECRET=your-own-secure-secret-key-here
mvn spring-boot:run
```
