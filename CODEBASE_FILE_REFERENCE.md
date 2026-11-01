# Codebase File Reference

This reference describes authored source, configuration, tests, and documentation. Maven output in `target/` is generated and is not source.

## Request and persistence flow

Browser JavaScript or an API client sends an HTTP request → a controller validates/routs it → a service applies inventory rules → a Spring Data JPA repository reads/writes an entity → Hibernate persists it in MySQL. `ApiExceptionHandler` maps expected exceptions to the existing JSON `message` response.

Stock operations run inside one transaction. `ProductRepository.findByIdForUpdate` takes a pessimistic write lock on the product row; the service updates the managed product and saves an `InventoryMovement`. A failure rolls back both writes. `Product.version` is also a Hibernate `@Version` field for optimistic version tracking.

## Root files and configuration

| File | Responsibility |
|---|---|
| `pom.xml` | Java 25/Spring Boot 3.5.16 build; Web, Validation, Data JPA, MySQL Connector/J, test, and H2 test dependencies. |
| `.gitignore` | Excludes Maven output, IDE files, local caches, and OS files. |
| `README.md` | Project setup, architecture, API, MySQL, and test instructions. |
| `PROJECT_GUIDE.md` | Learning-focused architecture and Spring/JPA walkthrough. |
| `USER_GUIDE.md` | Dashboard and API usage steps. |
| `CODEBASE_FILE_REFERENCE.md` | This file-by-file source reference. |
| `src/main/resources/application.properties` | Port 8080, MySQL environment configuration, Hibernate schema update, disabled Open Session in View, and strict integer JSON parsing. |
| `src/test/resources/application.properties` | In-memory H2 and `create-drop` schema for tests only; test runs do not need MySQL. |

Database configuration uses `DB_URL` (default `jdbc:mysql://localhost:3306/warehouse_inventory`), `DB_USERNAME`, and `DB_PASSWORD`. The username and password are required external values.

## Application entry point

| File | Responsibility |
|---|---|
| `src/main/java/com/warehouse/inventory/InventorySystemApplication.java` | `@SpringBootApplication` entry point; launches the server and component/repository scanning. |

## Controllers and exception handling

`@RestController` maps HTTP requests to Java methods and serializes returned DTOs/entities to JSON. Controllers delegate business behavior to services.

| File | Responsibility |
|---|---|
| `controller/ProductController.java` | `POST /api/products`, `GET /api/products`, and `GET /api/products/{id}`. Applies `@Valid` to product creation. |
| `controller/StockController.java` | Inward/outward routes at `/api/products/{id}/stock/...`; validates `StockOperationRequest` and calls `StockService`. |
| `controller/MovementController.java` | `GET /api/movements` and `GET /api/movements/product/{productId}`. Returns `MovementResponse` DTOs. |
| `controller/AlertController.java` | `GET /api/alerts/low-stock`. |
| `controller/ApiExceptionHandler.java` | `@RestControllerAdvice`; converts validation, malformed JSON, type mismatch, missing product, invalid operation, insufficient stock, and duplicate ID exceptions to HTTP errors with a JSON `message`. |

## Services

`@Service` marks Spring-managed application logic. Services coordinate persistence and enforce business rules.

| File | Responsibility |
|---|---|
| `service/ProductService.java` | Creates products with duplicate-ID prevention, lists them by ID, and retrieves individual products. Uses transactions for persistence. |
| `service/StockService.java` | Validates IDs and positive quantities; checks overflow/available stock; locks and updates the product; records the movement within the same `@Transactional` boundary. |
| `service/MovementService.java` | Persists movements and maps entities to the existing response shape; returns global or product-specific history newest first and checks product existence. |
| `service/AlertService.java` | Calculates alerts from products using the strict `quantity < reorderLevel` rule; does not store a separate alert collection. |

## JPA entities, repositories, and DTOs

`@Entity` types are mapped by Hibernate to relational tables. The API preserves its former JSON field names; the movement relationship is not serialized directly.

| File | Type and fields/responsibility |
|---|---|
| `model/Product.java` | JPA `products` entity: caller-supplied `id`, `name`, `description`, `quantity`, and `reorderLevel` mapped to `reorder_level`; validation annotations enforce required values, lengths, and nonnegative quantities. `version` is an internal `@Version` column ignored by JSON. |
| `model/InventoryMovement.java` | JPA `inventory_movements` entity: generated `id` (`movement_id`), `Product` relation, `type`, `quantity`, `timestamp`, `quantityBefore`, and `quantityAfter`. |
| `model/MovementType.java` | `INWARD` and `OUTWARD`; persisted as enum strings. |
| `model/MovementResponse.java` | JSON response DTO: `id`, `productId`, `type`, `quantity`, `timestamp`, `quantityBefore`, and `quantityAfter`. Avoids exposing the lazy JPA product relationship. |
| `model/LowStockAlert.java` | Alert response record: product ID/name, current quantity, reorder level, and explanation. |
| `model/StockOperationRequest.java` | Request record for stock endpoints; requires a positive quantity. |
| `repository/ProductRepository.java` | `JpaRepository<Product,Integer>`; ordered product query and `findByIdForUpdate` with `PESSIMISTIC_WRITE` locking. |
| `repository/MovementRepository.java` | `JpaRepository<InventoryMovement,Long>`; queries all movements and a product's movements in descending generated-ID order. |

### Database relationship

One product may have many movement rows; every movement must reference exactly one existing product (`inventory_movements.product_id` → `products.id`, `@ManyToOne`). A reverse collection is not required by the API and is not exposed.

## Exceptions

| File | API meaning |
|---|---|
| `exception/ProductNotFoundException.java` | Unknown product; HTTP 404. |
| `exception/DuplicateProductException.java` | Product ID already exists; HTTP 409. |
| `exception/InsufficientStockException.java` | Outward amount exceeds stock; HTTP 409. |
| `exception/InvalidStockOperationException.java` | Invalid stock ID/amount or integer overflow; HTTP 400. |

## Browser dashboard

Spring Boot serves these static resources; `/` is the welcome page and the API remains under `/api`.

| File | Responsibility |
|---|---|
| `src/main/resources/static/index.html` | Dashboard structure, statistics, product table, product/stock forms, alerts, movement filter, and details dialog. |
| `src/main/resources/static/style.css` | Responsive inventory dashboard styling. |
| `src/main/resources/static/script.js` | Uses `fetch()` for the existing REST endpoints, handles JSON errors/loading feedback, and refreshes rendered data after changes. |

The frontend formats/displays data but does not implement business rules or write to the database directly.

## Tests

| File | Coverage |
|---|---|
| `src/test/java/com/warehouse/inventory/ProductControllerTest.java` | Product endpoint status codes, JSON shapes, validation, and controller error handling. |
| `src/test/java/com/warehouse/inventory/WarehouseApiIntegrationTest.java` | Product creation/duplicate behavior, existing API workflows, stock, movements, alerts, errors, plus root dashboard/static asset routing. |
| `src/test/java/com/warehouse/inventory/repository/ProductRepositoryTest.java` | Product persistence, uniqueness, ordering, and retrieval after clearing the JPA persistence context. |
| `src/test/java/com/warehouse/inventory/repository/MovementRepositoryTest.java` | Movement persistence, product foreign-key association, and newest-first history. |
| `src/test/java/com/warehouse/inventory/service/StockServiceTest.java` | Inward/outward rules, invalid and excessive operations, concurrent outward requests, and history ordering/filtering. |
| `src/test/java/com/warehouse/inventory/service/StockTransactionRollbackTest.java` | Verifies product quantity rollback when recording a movement fails. |
| `src/test/java/com/warehouse/inventory/service/AlertServiceTest.java` | Strict low-stock threshold and empty/no-alert conditions. |

Run all tests and create the executable JAR with `mvn package`.
