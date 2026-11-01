# Warehouse Inventory User Guide

Use the browser dashboard for everyday inventory tasks or call the same endpoints from Postman or another HTTP client.

## Start the application

Start MySQL Server and create the database/account if not already configured:

```sql
CREATE DATABASE warehouse_inventory CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'warehouse_user'@'localhost' IDENTIFIED BY 'REPLACE_WITH_A_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON warehouse_inventory.* TO 'warehouse_user'@'localhost';
```

Then set environment variables in PowerShell and start Spring Boot from the folder containing `pom.xml`:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/warehouse_inventory"
$env:DB_USERNAME = "warehouse_user"
$env:DB_PASSWORD = "your-private-password"
mvn spring-boot:run
```

Open [http://localhost:8080/](http://localhost:8080/) in a browser. Data is stored in MySQL and remains available across application restarts as long as the same database is used.

## Use the dashboard

- **Overview:** total products, units in stock, low-stock products, and recorded movements.
- **Products:** see product ID, name, description, current quantity, reorder level, and stock status. Select **Details** to view an individual product.
- **Add a product:** enter a unique positive ID, name, description, starting quantity, and reorder level. Quantity and reorder level may be zero.
- **Update stock:** select a product, choose inward or outward, enter a positive whole-number quantity, and apply the operation. The dashboard refreshes after a successful change.
- **Movement history:** view all movements or select a product to request only its history. Each record shows type, quantity, timestamp, and before/after stock.
- **Low-stock alerts:** products appear when `quantity < reorderLevel`; matching rows and alerts are highlighted.

The dashboard displays backend errors, including duplicate IDs and attempts to remove more stock than is available.

## REST API reference

Base URL: `http://localhost:8080`

| Method | URL | Purpose |
|---|---|---|
| `POST` | `/api/products` | Add a product |
| `GET` | `/api/products` | List products |
| `GET` | `/api/products/{id}` | View one product |
| `PUT` | `/api/products/{id}/stock/inward` | Add stock |
| `PUT` | `/api/products/{id}/stock/outward` | Remove stock |
| `GET` | `/api/movements` | View all stock changes |
| `GET` | `/api/movements/product/{productId}` | View one product's changes |
| `GET` | `/api/alerts/low-stock` | View low-stock products |

### Example sequence

Create a product with `POST /api/products` and a JSON body:

```json
{
  "id": 101,
  "name": "Keyboard",
  "description": "Wireless keyboard",
  "quantity": 10,
  "reorderLevel": 12
}
```

Add five units with `PUT /api/products/101/stock/inward`:

```json
{"quantity": 5}
```

Then inspect the product and movement history:

```text
GET /api/products/101
GET /api/movements/product/101
GET /api/alerts/low-stock
```

Each successful stock change records one movement. Creating a product does not create a movement.

## Validation and common errors

| Status | Meaning |
|---|---|
| `400 Bad Request` | Missing/invalid field, blank name/description, non-positive stock amount, malformed JSON, or invalid ID. |
| `404 Not Found` | No product exists with the requested ID. |
| `409 Conflict` | Product ID already exists or requested outward amount exceeds available stock. |

Errors have a JSON `message` field. An outward operation cannot make stock negative; a rejected request leaves both quantity and movement history unchanged. Product IDs must be positive integers, and product names/descriptions have maximum lengths of 150/1000 characters.
