# Warehouse Inventory Management System

## Overview

This college mini-project provides a REST API for maintaining warehouse products, changing stock, viewing inventory movement history, and checking current low-stock alerts. It is a backend-only application. All data lives in memory for the lifetime of the process.

## Problem statement and objectives

Warehouse teams need a reliable way to identify products, update quantities, review stock changes, and spot items that have fallen below their reorder levels. The project provides a small validated API for those tasks while keeping the data model and storage simple enough to explain in a viva.

Project objectives:

- Manage a product catalogue with unique positive product IDs.
- Apply validated inward and outward stock changes without allowing negative stock or integer overflow.
- Record each successful stock change with its type, quantity, timestamp, and before/after quantities.
- Calculate low-stock alerts from the current shared product data.
- Keep all data in thread-safe in-memory collections without a database.

## Completed features and limitations

Completed and covered by automated tests:

- Create products and retrieve all products or one product by ID.
- Validate product fields and stock operation quantities.
- Reject duplicate product IDs, unknown stock targets, excessive outward operations, and stock overflow with meaningful status codes.
- Store movement history in newest-first order and filter it by product.
- Calculate low-stock alerts dynamically using `quantity < reorderLevel`.
- Protect stock changes with a synchronized service operation and roll back the quantity if movement recording fails.
- Keep product storage in one shared `ProductRepository` used by product, stock, movement, and alert features.

Limitations:

- Product and movement data are not persisted. **All data is lost when the application stops or restarts.**
- There is no authentication, authorization, product update/delete API, frontend, or external database.
- Stock changes are synchronized in one service instance. This protects concurrent requests in this single-process application, but it is not a distributed lock for multiple application instances.

## Technology stack

- Java 25
- Spring Boot 3.5.16
- Spring Web and Jakarta Bean Validation
- Maven
- Spring Boot Starter Test, JUnit 5, and MockMvc
- Java `ConcurrentHashMap`, `AtomicLong`, and `synchronized` for in-memory storage and stock coordination

No database, JPA, Hibernate, JDBC, ORM, or frontend framework is used.

## Architecture

- **Model:** `Product` is the single product record; `InventoryMovement` and `MovementType` describe completed stock changes; `LowStockAlert` and `StockOperationRequest` are API data shapes.
- **Repository:** `ProductRepository` owns the only product collection and performs atomic quantity replacement. `MovementRepository` owns movement records and allocates movement IDs safely.
- **Service:** `StockService` validates and coordinates stock changes and movement recording. `MovementService` records and queries history. `AlertService` calculates alerts from the current product repository contents.
- **Controller:** product, stock, movement, and alert controllers map HTTP requests to the shared services/repositories.
- **Exception handling:** `ApiExceptionHandler` returns a short JSON `message` for validation, malformed input, missing products, invalid quantities, and insufficient stock.

Every component uses constructor injection. Stock changes are synchronized by `StockService` across the quantity update and movement write. If movement recording throws a runtime exception, the previous quantity is restored before the error is propagated. Product data is copied at repository boundaries; movement records and alert records are immutable Java records. Movement lists are sorted and returned as snapshots.

## Project structure

```text
warehouse-inventory-system/
├── .gitignore
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/warehouse/inventory/
    │   │   ├── InventorySystemApplication.java
    │   │   ├── controller/
    │   │   │   ├── AlertController.java
    │   │   │   ├── ApiExceptionHandler.java
    │   │   │   ├── MovementController.java
    │   │   │   ├── ProductController.java
    │   │   │   └── StockController.java
    │   │   ├── exception/
    │   │   ├── model/
    │   │   ├── repository/
    │   │   └── service/
    │   └── resources/application.properties
    └── test/java/com/warehouse/inventory/
        ├── ProductControllerTest.java
        ├── WarehouseApiIntegrationTest.java
        ├── repository/ProductRepositoryTest.java
        └── service/
            ├── AlertServiceTest.java
            └── StockServiceTest.java
```

## Prerequisites and VS Code setup

- JDK 25.
- Apache Maven 3.6.3 or newer (the project was verified with Maven 3.10.0).
- Visual Studio Code and the Java Extension Pack are recommended.

Check installations with `java -version` and `mvn -version`. In VS Code, open the folder containing `pom.xml`, install/enable the Java Extension Pack if prompted, wait for the Maven import to finish, and use the integrated terminal from that project folder.

## Build, test, and run

Run from the folder containing `pom.xml`:

```bash
mvn clean test
mvn clean package
mvn spring-boot:run
```

Alternatively, run the packaged application:

```bash
java -jar target/warehouse-inventory-system-0.0.1-SNAPSHOT.jar
```

The default base URL is `http://localhost:8080`.

## Product API

Product JSON field names are `id`, `name`, `description`, `quantity`, and `reorderLevel`.

### Create a product

`POST /api/products`

```json
{
  "id": 101,
  "name": "Keyboard",
  "description": "Wireless keyboard",
  "quantity": 10,
  "reorderLevel": 12
}
```

Returns `201 Created` and the created product. A duplicate ID returns `409 Conflict`; invalid or missing fields return `400 Bad Request`.

### List products

`GET /api/products`

Returns `200 OK` and a JSON array in ascending product ID order. With no products, returns `[]`.

### Get a product

`GET /api/products/{id}`

For example, `GET /api/products/101` returns `200 OK` and the product. A non-positive or non-integer ID returns `400`; an unknown positive ID returns `404`.

## Stock API

Both stock operations accept a positive integer quantity and return the updated product with `200 OK`.

### Stock inward

`PUT /api/products/{id}/stock/inward`

Example: `PUT /api/products/101/stock/inward`

```json
{
  "quantity": 5
}
```

If the product had 10 units, the response product has 15 units. A request that would overflow the integer quantity returns `400`. An unknown product returns `404`.

### Stock outward

`PUT /api/products/{id}/stock/outward`

Example: `PUT /api/products/101/stock/outward`

```json
{
  "quantity": 3
}
```

The requested quantity may equal the available quantity, leaving stock at zero. Removing more than is available returns `409 Conflict`; invalid quantities return `400`; an unknown product returns `404`.

## Movement history API

Each movement response contains `id`, `productId`, `type`, `quantity`, `timestamp`, `quantityBefore`, and `quantityAfter`. Movement quantity is always positive; `type` is `INWARD` or `OUTWARD`. Results are ordered newest first by the generated movement ID.

### List all movements

`GET /api/movements`

Returns `200 OK` with a JSON array, or `[]` when no stock changes have succeeded.

Example movement:

```json
{
  "id": 2,
  "productId": 101,
  "type": "OUTWARD",
  "quantity": 3,
  "timestamp": "2026-10-22T16:30:00Z",
  "quantityBefore": 15,
  "quantityAfter": 12
}
```

### List movements for one product

`GET /api/movements/product/{productId}`

Returns `200 OK` with only that product's history. An existing product with no movements returns `[]`; an unknown product returns `404`; a non-positive ID returns `400`.

## Low-stock alerts API

`GET /api/alerts/low-stock`

Returns `200 OK` and a JSON array calculated from current products. Products appear only when `quantity < reorderLevel`; equality is not considered low stock. The response contains `productId`, `productName`, `quantity`, `reorderLevel`, and `message`. No alerts returns `[]`.

Example:

```json
{
  "productId": 101,
  "productName": "Keyboard",
  "quantity": 7,
  "reorderLevel": 12,
  "message": "Low stock: Keyboard has 7 units; reorder level is 12"
}
```

## Validation and error responses

- Product ID is required and greater than zero.
- Product name and description are required and nonblank.
- Product quantity and reorder level are required and nonnegative.
- Stock operation quantity is required and greater than zero.
- Duplicate product IDs return `409`.
- Missing products return `404`.
- Invalid stock quantities, malformed JSON, and stock overflow return `400`.
- Outward quantity above available stock returns `409` and does not alter stock or history.

Errors use the same simple JSON shape, for example:

```json
{
  "message": "Product 101 has 7 available; cannot remove 8"
}
```

Stack traces and internal implementation details are not returned in these expected error responses.

## Postman testing and demonstration sequence

Set the request base URL to `http://localhost:8080`. For JSON requests, select **Body → raw → JSON**.

1. `POST /api/products` with the sample product above; expect `201`.
2. `GET /api/products` and `GET /api/products/101`; expect `200` and quantity 10.
3. `PUT /api/products/101/stock/inward` with `{"quantity":5}`; expect `200` and quantity 15.
4. `PUT /api/products/101/stock/outward` with `{"quantity":8}`; expect `200` and quantity 7.
5. `GET /api/movements` and `GET /api/movements/product/101`; expect two records with before/after values 10→15 and 15→7.
6. `GET /api/alerts/low-stock`; with reorder level 12 and quantity 7, expect one alert.
7. Repeat the product POST; expect `409` for duplicate ID.
8. Try stock inward with `{"quantity":0}`, `{"quantity":-1}`, `{}`, or malformed JSON; expect `400`.
9. Try stock outward with `{"quantity":999}`; expect `409`. Confirm product quantity and movement count did not change.
10. Try a stock request or product-specific movement request for an unknown ID; expect `404`.

Requests that change data affect only the current app run because storage is in memory. Restarting the application clears products and movements.

## Four-member team plan

This is the suggested division of project responsibility; it does not claim that four people made contributions to this implementation.

1. **Member 1 — Product Management:** `Product`, `ProductRepository`, `ProductController`, and Phase 1 product tests.
2. **Member 2 — Stock Management:** stock inward/outward behavior, validation, and stock tests.
3. **Member 3 — Movement Tracking:** movement model, enum, repository, service, controller, and history tests.
4. **Member 4 — Low-Stock Alerts and Integration:** alert service/controller, cross-module integration tests, README updates, and final verification.

## Git/GitHub collaboration

Use focused feature branches and small descriptive commits. Review changes through pull requests, resolve conflicts with the relevant contributors, and run `mvn clean test` before merging. Configure a remote only after the team has created and authorized the GitHub repository; no remote, commit, or push is claimed here.

Suggested commands after reviewing `git status`:

```bash
git add .
git commit -m "Complete Phase 2 inventory operations"
git push -u origin <branch-name>
```

## Future improvements

Possible later work includes durable persistence, authentication and authorization, product update/delete operations, pagination for large histories, and distributed coordination if the service is deployed with multiple instances. These items are not implemented in this phase.
