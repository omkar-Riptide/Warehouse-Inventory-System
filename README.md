# Warehouse Inventory Management System

## Project overview

The Warehouse Inventory Management System is a beginner-friendly Java REST API project. Phase 1 provides the product-management foundation: create a product and retrieve products by list or ID. Products are held in memory while the application runs.

## Problem statement

Warehouses need a consistent way to identify products and see their current quantities and reorder levels. This phase establishes a validated product catalogue through a small HTTP API before stock operations and related tracking are added in a later phase.

## Phase 1 objectives

- Start a Spring Boot application on port 8080.
- Create products with validated fields and unique positive IDs.
- Retrieve all products in ascending ID order and retrieve one product by ID.
- Return useful HTTP status codes for successful requests and errors.
- Keep product storage in a Spring-managed, thread-safe in-memory repository.

## Objectives completed in Phase 1

- Implemented `POST /api/products`, `GET /api/products`, and `GET /api/products/{id}`.
- Added request validation for required fields and numeric ranges.
- Added atomic duplicate-ID rejection and defensive copies in the repository.
- Added consistent JSON error messages for validation, unreadable request bodies, and expected HTTP errors.
- Added repository unit tests and MVC tests for the API behavior described below.

## Technology stack

- Java 25 language/runtime target
- Spring Boot 3.5.16, Spring Web, and Spring Boot Validation
- Maven
- JUnit 5 and Spring Boot Test
- `ConcurrentHashMap` for in-memory product storage

There is no database, ORM, frontend, or third-party persistence layer in this phase.

## System architecture

- **Controller (`ProductController`)**: maps HTTP requests to repository operations, validates request bodies, and selects response status codes.
- **Repository (`ProductRepository`)**: owns the only product collection, rejects duplicate IDs atomically, returns sorted snapshots, and copies products at its boundaries.
- **Model (`Product`)**: represents the five product fields and declares Jakarta Bean Validation rules.
- **API exception handler (`ApiExceptionHandler`)**: turns validation, malformed JSON, and expected status errors into short JSON messages.

Spring constructor injection connects the controller and repository. The application class is in the base package so component scanning discovers these classes.

## Project directory structure

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
    │   │   │   ├── ApiExceptionHandler.java
    │   │   │   └── ProductController.java
    │   │   ├── model/Product.java
    │   │   └── repository/ProductRepository.java
    │   └── resources/application.properties
    └── test/java/com/warehouse/inventory/
        ├── ProductControllerTest.java
        └── repository/ProductRepositoryTest.java
```

## Prerequisites

- Java Development Kit 25 or newer. The Maven compiler targets Java 25.
- Apache Maven 3.6.3 or newer.
- Visual Studio Code and the Java Extension Pack are recommended for editing.

Check the tools in a terminal with `java -version` and `mvn -version`.

## Open in VS Code

1. Open VS Code and choose **File → Open Folder**.
2. Select the folder containing this `README.md` and `pom.xml`.
3. Install or enable the Java Extension Pack if VS Code prompts you.
4. Allow the Java language server to import the Maven project.
5. Open a VS Code terminal in the project folder before running the commands below.

## Build, test, and run

Run these commands from the directory containing `pom.xml`:

```bash
mvn clean test
mvn clean package
mvn spring-boot:run
```

The packaged application can also be started with:

```bash
java -jar target/warehouse-inventory-system-0.0.1-SNAPSHOT.jar
```

The application listens on `http://localhost:8080` by default. Maven downloads the declared dependencies on the first build when they are not already cached.

## API documentation

Base URL: `http://localhost:8080`

All product JSON property names are camel case and match the Java fields: `id`, `name`, `description`, `quantity`, and `reorderLevel`.

### Create a product

`POST /api/products`

Purpose: add a product with a unique positive ID.

Example request:

```json
{
  "id": 101,
  "name": "Keyboard",
  "description": "Wireless keyboard",
  "quantity": 50,
  "reorderLevel": 10
}
```

Expected responses:

- `201 Created` with the created product as JSON.
- `400 Bad Request` with a `message` for invalid fields or malformed JSON.
- `409 Conflict` with a `message` if that product ID already exists.

Representative `201` response:

```json
{
  "id": 101,
  "name": "Keyboard",
  "description": "Wireless keyboard",
  "quantity": 50,
  "reorderLevel": 10
}
```

### Retrieve all products

`GET /api/products`

Purpose: list products in ascending ID order.

- `200 OK` with a JSON array.
- If no products exist, the response body is `[]`.

### Retrieve one product

`GET /api/products/{id}`

Purpose: retrieve the product with the supplied ID.

- `200 OK` with the product JSON when it exists.
- `400 Bad Request` with a `message` for a non-positive ID or a non-integer path value.
- `404 Not Found` with a `message` when a positive ID does not exist.

Example: `GET http://localhost:8080/api/products/101`.

## Validation and error behavior

- `id` is required and must be greater than zero.
- `name` and `description` are required and cannot be blank.
- `quantity` and `reorderLevel` are required and must be zero or greater.
- A duplicate ID is rejected with HTTP 409; concurrent inserts cannot both claim the same ID.
- Invalid request bodies and malformed JSON receive HTTP 400 and a JSON body such as `{"message":"..."}`.
- An unknown positive product ID receives HTTP 404. Error responses do not include stack traces.

## Postman testing instructions

1. Start the application using `mvn spring-boot:run` and wait for it to report that it started on port 8080.
2. Create a request with method `POST` and URL `http://localhost:8080/api/products`.
3. Choose **Body → raw → JSON**, paste the example product request above, and send it. Expect `201`.
4. Send the same request again. Expect `409` because IDs must be unique.
5. Send `GET http://localhost:8080/api/products` and `GET http://localhost:8080/api/products/101`. Expect `200` responses.
6. Try `GET http://localhost:8080/api/products/99999`. Expect `404` unless that ID was created in the current run.
7. Try a POST with a blank `name`, a missing `description`, `id: 0`, a negative `quantity`, or a negative `reorderLevel`. Expect `400`.
8. Try malformed JSON such as `{bad json`. Expect `400` with a message.

## In-memory storage and known limitations

The repository stores products in a thread-safe `ConcurrentHashMap` for the lifetime of the running application. It returns products in predictable ascending ID order and makes defensive copies so callers cannot mutate stored objects through returned references. **All product data is lost when the application stops or restarts.** There is no persistence, authentication, update endpoint, delete endpoint, stock movement, or frontend in Phase 1.

## Phase 2 roadmap (planned, not implemented)

The next phase is intended to add:

- Stock inward through `PUT /api/products/{id}/stock/inward`.
- Stock outward through `PUT /api/products/{id}/stock/outward`.
- Validation for stock changes that prevents invalid quantities and negative inventory.
- Minimal inventory movement records containing the product, movement type, quantity, timestamp, and any other justified fields.
- All movement history through `GET /api/movements`.
- Product-specific movement history through `GET /api/movements/product/{productId}`.
- Low-stock alerts through `GET /api/alerts/low-stock`, using `quantity < reorderLevel`.
- Integration tests for stock changes, movement recording, and alerts.
- Updated Postman examples, API documentation, and this README.

**Shared-data design rule:** all Phase 2 modules must reuse the same Spring-managed `ProductRepository` and `Product` model. Stock, movement, and alert modules must not create independent copies of inventory data.

Planned four-member division:

1. **Member 1:** Product Management.
2. **Member 2:** Stock Management.
3. **Member 3:** Movement Tracking.
4. **Member 4:** Low-Stock Alerts and Integration.

These are planned Phase 2 responsibilities; none of the Phase 2 endpoints or behaviors are implemented in this project version.

## Git and GitHub team contribution guidance

Git is available for local version control. For a four-member team, agree on a shared branch naming convention, assign each member a focused feature branch, and use pull requests for review before merging to the shared default branch. Keep commits small and descriptive, resolve conflicts together, and run `mvn clean test` before merging. Add a GitHub remote and push only after the team creates and authorizes the repository; this project does not claim that a remote is configured.

Suggested initial local commands (review the staged files before committing):

```bash
git status
git add .
git commit -m "Create Phase 1 warehouse inventory API"
```
