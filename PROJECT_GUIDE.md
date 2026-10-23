# Warehouse Inventory Project Guide

This guide explains what the project is for, how a request moves through it, and what each source, test, and configuration file does.

## 1. What the project does

This project is a small backend service for keeping track of warehouse inventory. It exposes a REST API (an HTTP interface that sends and receives JSON) to:

- Create products and look them up.
- Add stock or remove stock while checking that the requested change is valid.
- Keep a history of successful stock changes.
- Show products whose quantity is below their reorder level.

The project demonstrates how a Java Spring Boot application can split web requests, business rules, and data storage into separate parts. It stores data in memory, so products and movements are cleared when the application stops. It does not include a web page, login system, or database.

## 2. Workflow: what happens when someone uses the API

1. A client such as Postman sends an HTTP request to the running application. The request may contain JSON, for example a product or a quantity to add.
2. A **controller** matches the URL and HTTP method, reads the request, and calls the relevant application code.
3. For a stock change, `StockService` checks the product ID and quantity, calculates the new stock, updates the shared product repository, and records a movement with the before and after quantities. If recording fails, it restores the old quantity. Invalid requests do not create movements.
4. A **repository** stores or retrieves the data. Here the repositories use thread-safe in-memory collections, not a database.
5. Spring converts the result to JSON and sends an HTTP response. If something goes wrong, `ApiExceptionHandler` turns known errors into a status code and a short JSON `message`.

Low-stock alerts are calculated from the current product list each time they are requested. A product is low when its quantity is strictly less than its reorder level; being exactly at the reorder level is not an alert.

### Example stock-change flow

If product 101 has 10 units and a client sends `PUT /api/products/101/stock/inward` with `{"quantity":5}`, the controller passes the request to `StockService`. The service validates it, changes the quantity to 15, and records an `INWARD` movement from 10 to 15. The response contains the updated product. A later movement-history request can show the recorded change.

## 3. Main parts of the application

The Java code is under `src/main/java/com/warehouse/inventory/`:

- **Controller:** HTTP endpoints and request/response handling.
- **Service:** application rules and coordination between repositories.
- **Repository:** in-memory data access.
- **Model:** the data objects passed around or returned as JSON.
- **Exception:** named error types for expected failure cases.

These parts are connected by Spring Boot dependency injection. For example, a controller receives the service or repository it needs through its constructor; Spring creates and supplies those objects when the application starts.

## 4. File-by-file guide

### Project-root files

| File | What it does |
|---|---|
| `pom.xml` | Maven's project recipe. It gives the project its name and version, selects Java 25, imports Spring Boot's dependency/version defaults, lists the libraries needed by the app and tests, and configures the Spring Boot Maven plugin so Maven can run and package the application. Details are below. |
| `README.md` | Project overview, setup and run instructions, API examples, limitations, and a demonstration sequence. It is the quick-start and usage document. |
| `PROJECT_GUIDE.md` | This guide: a longer explanation of the workflow, architecture, and individual files. |
| `.gitignore` | Tells Git to leave build output, local Maven cache, IDE settings, logs, and operating-system clutter out of version control. It does not delete those files. |

### Application entry point and settings

| File | What it does |
|---|---|
| `src/main/java/com/warehouse/inventory/InventorySystemApplication.java` | The program's entry point. Its `main` method starts Spring Boot. `@SpringBootApplication` tells Spring to configure the app and find its components, including controllers, services, and repositories under this package. |
| `src/main/resources/application.properties` | Runtime settings. `server.port=8080` makes the API listen on port 8080. `spring.jackson.deserialization.accept-float-as-int=false` prevents JSON decimal values such as `1.5` from being silently accepted as integers. |

### Controllers: the HTTP interface

| File | What it does |
|---|---|
| `controller/ProductController.java` | Handles `/api/products`: creates a product (`POST`), lists products (`GET`), and retrieves one by ID (`GET`). It rejects duplicate IDs and returns the created status for successful creation. |
| `controller/StockController.java` | Handles stock inward and outward requests at `/api/products/{id}/stock/inward` and `/api/products/{id}/stock/outward`. It validates the request body and delegates stock rules to `StockService`. |
| `controller/MovementController.java` | Handles `/api/movements` and `/api/movements/product/{productId}` to return all movement history or one product's history. |
| `controller/AlertController.java` | Handles `GET /api/alerts/low-stock` and returns the current low-stock alerts. |
| `controller/ApiExceptionHandler.java` | Central place for converting validation failures, unreadable JSON, invalid IDs, missing products, invalid stock operations, and insufficient stock into useful HTTP status codes and JSON messages. |

### Services: the application rules

| File | What it does |
|---|---|
| `service/StockService.java` | Applies stock rules. It checks for a real product and a positive quantity, prevents stock from going below zero or exceeding the integer limit, updates the quantity, and records the movement. Stock operations are synchronized within this running application. If movement recording fails after the update, it attempts to restore the previous quantity. |
| `service/MovementService.java` | Connects movement history to the rest of the app. It records movements and retrieves them. Before returning one product's history, it checks that the ID is positive and that the product exists. |
| `service/AlertService.java` | Reads the current products, selects those with `quantity < reorderLevel`, and makes a readable alert for each one. It does not store a separate alert list. |

### Repositories: in-memory storage

| File | What it does |
|---|---|
| `repository/ProductRepository.java` | Owns the shared product collection. It adds unique products, checks IDs, retrieves products in ID order, and replaces a quantity only if the saved quantity still matches the expected old value. It returns copies of products so callers cannot accidentally change stored objects. |
| `repository/MovementRepository.java` | Owns the movement collection. It assigns increasing movement IDs, timestamps each movement, and returns all or product-filtered movements newest first. |

### Models: the data shapes

| File | What it does |
|---|---|
| `model/Product.java` | Product data: `id`, `name`, `description`, `quantity`, and `reorderLevel`. Validation annotations require a positive ID, nonblank name and description, and nonnegative quantity and reorder level. It is a mutable Java class because Spring reads JSON into it and stock operations update quantities. |
| `model/StockOperationRequest.java` | The JSON body for inward and outward operations. It contains one `quantity` field and requires that quantity to be present and greater than zero. |
| `model/InventoryMovement.java` | Immutable record describing a successful stock change: movement ID, product ID, type, amount, timestamp, quantity before, and quantity after. |
| `model/MovementType.java` | The two allowed movement labels: `INWARD` and `OUTWARD`. |
| `model/LowStockAlert.java` | Immutable record returned for a low-stock product: product ID and name, current quantity, reorder level, and explanatory message. |

### Exceptions: expected error types

| File | What it does |
|---|---|
| `exception/ProductNotFoundException.java` | Represents a request for a product ID that is not in the repository. The API handler turns it into `404 Not Found`. |
| `exception/InsufficientStockException.java` | Represents an attempt to remove more units than are available. The API handler turns it into `409 Conflict`. |
| `exception/InvalidStockOperationException.java` | Represents invalid stock-operation inputs such as a nonpositive product ID or quantity. The API handler turns it into `400 Bad Request`. |

## 5. What `pom.xml` does

`pom.xml` is the configuration file for **Maven**, the project's build tool. Maven reads it to know how to compile, run, test, and package the Java project.

- **Parent (`spring-boot-starter-parent`):** supplies Spring Boot's standard Maven configuration and default versions for compatible libraries. This project specifies Spring Boot version `3.5.16`.
- **Coordinates:** `groupId`, `artifactId`, and `version` identify the project to Maven. Together they form the name of the built artifact, `warehouse-inventory-system-0.0.1-SNAPSHOT.jar`.
- **Properties:** `java.version` is set to `25`, telling the build to use Java 25 settings.
- **Dependencies:**
  - `spring-boot-starter-web` provides the web server and tools for making REST endpoints.
  - `spring-boot-starter-validation` enables validation annotations such as `@NotBlank` and `@Min`.
  - `spring-boot-starter-test` provides test tools such as JUnit, Spring's test support, and MockMvc. Its `test` scope means it is used for tests, not as a runtime application library.
- **Spring Boot Maven plugin:** supports running the app with Maven and packaging it as an executable Spring Boot JAR.

For example, `mvn spring-boot:run` uses Maven and this configuration to start the app. `mvn package` compiles the project and builds the JAR under `target/`.

## 6. API at a glance

The default base address is `http://localhost:8080`.

| Method and path | Purpose |
|---|---|
| `POST /api/products` | Create a product from JSON. |
| `GET /api/products` | List products, ordered by ID. |
| `GET /api/products/{id}` | Get one product. |
| `PUT /api/products/{id}/stock/inward` | Add a positive quantity from a JSON body such as `{"quantity":5}`. |
| `PUT /api/products/{id}/stock/outward` | Remove a positive quantity, provided enough stock exists. |
| `GET /api/movements` | List successful stock changes, newest first. |
| `GET /api/movements/product/{productId}` | List successful changes for an existing product. |
| `GET /api/alerts/low-stock` | Calculate alerts from current inventory. |

Common status codes are `200` for successful reads and stock operations, `201` for product creation, `400` for invalid input, `404` when a product does not exist, and `409` for a duplicate ID or insufficient stock. Error responses use a JSON object with a `message` field.

## 7. Test files: what behavior they cover

Tests are under `src/test/java/com/warehouse/inventory/`. They are executable checks of expected behavior, not part of the running API.

| File | What it checks |
|---|---|
| `ProductControllerTest.java` | Product HTTP endpoints: create, list, retrieve, duplicate IDs, invalid product fields and IDs, malformed JSON, and error responses. It uses a mocked repository so it can focus on the controller. |
| `WarehouseApiIntegrationTest.java` | A full Spring application flow through MockMvc: create a product, update stock, inspect movement history and alerts, and check invalid, missing, overdrawn, and overflow requests. |
| `repository/ProductRepositoryTest.java` | Unique product IDs, missing IDs, sorted lists, and defensive copies that prevent outside edits from changing stored data. |
| `repository/MovementRepositoryTest.java` | Unique movement IDs and history results behaving as read-only snapshots. |
| `service/StockServiceTest.java` | Stock arithmetic and movement recording, rejected operations, rollback when recording fails, concurrency behavior, and newest-first/product-filtered history. |
| `service/AlertServiceTest.java` | The strict below-reorder-level rule and alerts changing as stock changes. |

To run the tests, use `mvn test` from the project folder. To build the packaged app, use `mvn package`. The app can be started in development with `mvn spring-boot:run`.

## 8. Important limits to keep in mind

- Products and movements exist only in memory. Restarting the app clears all data.
- This is a backend API; there is no user interface or authentication.
- Stock updates are synchronized inside one running application instance. That does not coordinate stock changes across multiple separate app instances.
- The API currently creates and reads products but does not provide product edit or delete endpoints.

