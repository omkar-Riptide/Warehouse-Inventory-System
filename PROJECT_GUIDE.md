# Warehouse Inventory Project Guide

This guide explains the system as a learning project: where a request enters, how Spring delegates it, how JPA saves data, and how the dashboard uses the API.

## 1. Application workflow

The application has two ways to start a request:

1. A browser loads `/` and receives the static dashboard (`index.html`, `style.css`, and `script.js`), or an API client sends a direct HTTP request.
2. Dashboard JavaScript calls the same `/api/...` endpoints as other clients using `fetch()`.
3. A controller maps the URL and method, binds JSON to a model/request DTO, and applies Jakarta Bean Validation with `@Valid`.
4. The controller delegates to a service. Services enforce inventory rules and coordinate related repository operations.
5. Spring Data JPA repositories load or save entities. Hibernate translates the entity operations into SQL for MySQL.
6. Spring serializes response objects to JSON. Expected failures are mapped by `ApiExceptionHandler` to a status code and a `{"message":"..."}` response.

The dashboard does not keep a separate authoritative copy of business rules. It shows server data and submits operations to the API.

## 2. System architecture and dependency injection

The source is divided into:

- **Controller** — HTTP route and request/response boundary.
- **Service** — business rules and transaction boundaries.
- **Repository** — JPA-based persistence interface.
- **Model** — persistent entities and API request/response data.
- **Exception** — named business failures mapped to HTTP errors.
- **Static resources** — HTML, CSS, and JavaScript served by Spring Boot.

Spring creates these components and supplies constructor dependencies (constructor injection). For example, `ProductController` receives `ProductService`, and `ProductService` receives `ProductRepository`.

## 3. Useful Spring annotations

- `@SpringBootApplication` starts auto-configuration and component scanning.
- `@RestController` declares HTTP handlers whose return values become response bodies.
- `@RequestMapping`, `@GetMapping`, `@PostMapping`, and `@PutMapping` associate URLs and HTTP methods with controller methods.
- `@RequestBody` binds JSON to a Java object; `@PathVariable` reads an ID from the URL.
- `@Valid` invokes Jakarta Bean Validation constraints such as `@NotBlank`, `@Min`, and `@Size`.
- `@Service` identifies application/business logic.
- `@RestControllerAdvice` and `@ExceptionHandler` centralize API errors.
- `@Transactional` groups data operations into a database transaction; `readOnly = true` marks query-only service methods.

## 4. JPA, Hibernate, and the relational schema

Jakarta Persistence (JPA) defines annotations and APIs for mapping Java entities to database tables. Hibernate is the JPA provider used here: it generates SQL and tracks entity changes. Spring Data JPA supplies repository interfaces and common operations such as `save`, `findById`, and `existsById`.

### `Product` → `products`

`Product` is an `@Entity` mapped to `products`. Its caller-supplied `id` is the primary key, preserving the existing API; it is deliberately not auto-generated. Other fields are `name`, `description`, `quantity`, and `reorderLevel` (`reorder_level` in SQL). Validation prevents blank/oversized text and negative quantities.

Hibernate's `@Version` field supports version tracking and is omitted from JSON. The service also obtains a pessimistic row lock for each stock change to coordinate simultaneous requests.

### `InventoryMovement` → `inventory_movements`

Each successful stock adjustment creates an entity with a database-generated `movement_id`, movement type, quantity, timestamp, and before/after quantities. `@ManyToOne` and `@JoinColumn(name="product_id")` establish the required foreign key to one product. The Java timestamp is stored as `movement_timestamp`.

The relationship is lazy and movement endpoints return the `MovementResponse` DTO rather than the entity. That keeps the API's stable `productId` field and avoids serializing a JPA relationship.

## 5. CRUD and stock operation flow

### Create and read products

`POST /api/products` accepts `id`, `name`, `description`, `quantity`, and `reorderLevel`. The controller validates the request. `ProductService` checks the ID and saves the product; the database primary key also prevents duplicates in a race. Product listing is ordered by ID; lookup of an unknown product returns 404.

### Stock inward/outward

`PUT /api/products/{id}/stock/inward` and `/outward` accept `{"quantity":N}`. `StockService`:

1. Checks a positive product ID and positive requested amount.
2. Loads the row using `findByIdForUpdate`, which requests a pessimistic write lock.
3. Rejects integer overflow for inward movement or insufficient quantity for outward movement.
4. Updates the managed product and persists an `InventoryMovement`.
5. Commits both changes together when the transaction succeeds.

If movement persistence fails, Spring rolls back the product update as well. A database lock, rather than a Java `synchronized` block, coordinates stock changes for transactions across application threads.

### Movement history and low-stock alerts

`MovementService` returns movement history newest first and verifies that a product exists for a product-specific query. The API DTO retains the original response fields: `id`, `productId`, `type`, `quantity`, `timestamp`, `quantityBefore`, and `quantityAfter`.

`AlertService` selects current products where `quantity < reorderLevel`. No separate alert table is needed because alerts are derived from current product records.

## 6. Browser-to-backend communication

`index.html` holds the dashboard structure; `style.css` provides responsive layout; `script.js` calls:

- `GET /api/products`
- `GET /api/alerts/low-stock`
- `GET /api/movements`
- `GET /api/movements/product/{productId}` when a history filter is selected
- `GET /api/products/{id}` for product details
- `POST /api/products` for product creation
- `PUT /api/products/{id}/stock/inward` or `/outward` for stock changes

Fetch failures display the API's `message` when available. Successful writes reload the dashboard data. Browser validation improves form usability, but the backend remains responsible for enforcing the rules.

## 7. Database setup and running

Install/start MySQL Server, then create the database and application account:

```sql
CREATE DATABASE warehouse_inventory CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'warehouse_user'@'localhost' IDENTIFIED BY 'REPLACE_WITH_A_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON warehouse_inventory.* TO 'warehouse_user'@'localhost';
```

In PowerShell, configure credentials and run:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/warehouse_inventory"
$env:DB_USERNAME = "warehouse_user"
$env:DB_PASSWORD = "your-private-password"
mvn spring-boot:run
```

`DB_URL` has the shown default; `DB_USERNAME` and `DB_PASSWORD` must be supplied. The application uses Hibernate `ddl-auto=update` for development. Do not commit credentials. Open `http://localhost:8080/` for the dashboard; API clients can use the endpoints in `USER_GUIDE.md`.

Tests use the separate H2 in-memory configuration in `src/test/resources/application.properties`, with `create-drop` schema. Run `mvn test` or `mvn package`; these test commands do not connect to MySQL.

## 8. Project files

See [CODEBASE_FILE_REFERENCE.md](CODEBASE_FILE_REFERENCE.md) for all controllers, services, repositories, entities, DTOs, exceptions, static files, configuration, and tests.

## 9. Scope and limitations

The API creates and reads products but has no product update/delete routes, authentication, or authorization. The normal application requires a running MySQL server and valid environment variables. Hibernate schema update is for local development rather than production migration management.

## 10. Step-by-step project demonstration

Follow these steps to demonstrate the running application in a browser and in Postman. MySQL Server must be installed, running, and configured first, as explained in section 7. The database is persistent, so choose a product ID that you have not already used; the examples use `901`.

### A. Start the application

1. Open a PowerShell terminal in the project folder (the folder containing `pom.xml`).
2. Set the database connection values in that same terminal. Use the username/password you configured for MySQL:

   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3306/warehouse_inventory"
   $env:DB_USERNAME = "warehouse_user"
   $env:DB_PASSWORD = "your-private-password"
   ```

3. Start Spring Boot:

   ```powershell
   mvn spring-boot:run
   ```

4. Wait for the startup message indicating that the application has started. Keep this terminal open while demonstrating. If startup reports a database connection error, check that MySQL is running and that the database name and credentials are correct.

### B. Show the browser dashboard

1. Open a browser and enter:

   ```text
   http://localhost:8080/
   ```

2. Point out the dashboard statistics, product table, low-stock alerts, and movement history.
3. In **Add a product**, enter ID `901`, name `Demo Keyboard`, description `Wireless keyboard for project demonstration`, starting quantity `10`, and reorder level `12`. Select **Add product**.
4. Show the new product in the table and the low-stock alert: quantity `10` is below reorder level `12`.
5. In **Update stock**, select **Demo Keyboard**, choose **Stock inward**, enter `5`, and select **Apply stock change**. The quantity becomes `15`; the dashboard refreshes and the low-stock alert clears.
6. Choose **Stock outward**, enter `8`, and apply it. The quantity becomes `7`; the low-stock alert returns.
7. Point out the two movement-history entries and their before/after values (`10 → 15` and `15 → 7`). Use the product filter to show only this product's movements. Select **Details** in the product table to display its details.

### C. Demonstrate the REST API in Postman

Use `http://localhost:8080` as the base URL. For requests with a body, choose **Body → raw → JSON** and set `Content-Type: application/json`.

1. **Check the initial product list.** Set the method to `GET`, paste this URL, and select **Send**:

   ```text
   http://localhost:8080/api/products
   ```

   The response is a JSON array. It may contain products from earlier demonstrations because MySQL data persists.

2. **Create the demonstration product.** Set the method to `POST` and paste:

   ```text
   http://localhost:8080/api/products
   ```

   In the JSON body, enter:

   ```json
   {
     "id": 901,
     "name": "Demo Keyboard",
     "description": "Wireless keyboard for project demonstration",
     "quantity": 10,
     "reorderLevel": 12
   }
   ```

   Select **Send**. Expect `201 Created` and the product JSON. If ID `901` is already present, choose a different unused positive ID and substitute it in the later URLs.

3. **Retrieve the product.** Set the method to `GET`, paste `http://localhost:8080/api/products/901`, and select **Send**. Expect `200 OK`, quantity `10`, and reorder level `12`.

4. **Add stock.** Set the method to `PUT`, paste `http://localhost:8080/api/products/901/stock/inward`, enter `{"quantity":5}` as the JSON body, and select **Send**. Expect `200 OK` and quantity `15`.

5. **Remove stock.** Set the method to `PUT`, paste `http://localhost:8080/api/products/901/stock/outward`, enter `{"quantity":8}`, and select **Send**. Expect `200 OK` and quantity `7`.

6. **Show movement history.** Set the method to `GET`, paste `http://localhost:8080/api/movements`, and select **Send**. The newest movement is first. Each movement shows its ID, product ID, type, quantity, timestamp, and before/after quantities.

7. **Filter movement history.** Set the method to `GET`, paste `http://localhost:8080/api/movements/product/901`, and select **Send**. Expect the two changes for the demonstration product.

8. **Show low-stock alerts.** Set the method to `GET`, paste `http://localhost:8080/api/alerts/low-stock`, and select **Send**. The product appears because its current quantity (`7`) is less than its reorder level (`12`).

9. **Demonstrate insufficient-stock validation.** Set the method to `PUT`, paste `http://localhost:8080/api/products/901/stock/outward`, and use body `{"quantity":8}`. Expect `409 Conflict` with a JSON `message`. Repeat the product and movement GET requests to show that neither quantity nor history changed.

For any different demonstration product ID, replace `901` consistently in the URLs and request bodies. Do not re-send the create request with an ID that already exists; duplicate IDs also return `409 Conflict`.

### D. Stop the application

Return to the terminal running Spring Boot and press **Ctrl+C**. Product and movement records remain in MySQL and can be retrieved the next time the application starts with the same database configuration.
