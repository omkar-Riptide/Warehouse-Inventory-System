# How to Use the Warehouse Inventory API

This guide shows how to start the inventory application and use it from a tool such as Postman. You can also send the same HTTP requests from another program.

## 1. Start the application

Open a terminal in the folder that contains `pom.xml`. Start the API with:

```bash
mvn spring-boot:run
```

Wait until Spring Boot reports that it has started. The API is then available at:

```text
http://localhost:8080
```

Keep the terminal open while using the API. To stop the app, press `Ctrl+C` in that terminal.

In Postman, create a request, choose its HTTP method, and enter the full URL from the examples below. For requests with a JSON body, choose **Body > raw > JSON**.

## 2. Add a product to inventory

Send a `POST` request to:

```text
http://localhost:8080/api/products
```

Use a JSON body like this:

```json
{
  "id": 101,
  "name": "Keyboard",
  "description": "Wireless keyboard",
  "quantity": 10,
  "reorderLevel": 12
}
```

The fields mean:

- `id`: a unique positive number for this product.
- `name`: the product's name.
- `description`: a short description.
- `quantity`: how many units are currently in stock; it can be zero but not negative.
- `reorderLevel`: the level below which this product should appear in low-stock alerts; it can be zero but not negative.

On success, the API returns `201 Created` and the saved product. Keep the product ID handy because stock changes and product lookup use it. Sending another product with the same ID returns `409 Conflict`.

## 3. Check inventory

To list all products, send:

```text
GET http://localhost:8080/api/products
```

To look up one product, send:

```text
GET http://localhost:8080/api/products/101
```

The list is ordered by product ID. A product lookup returns `404 Not Found` if that ID has not been added.

## 4. Add more stock (stock inward)

When new units arrive, send a `PUT` request to:

```text
http://localhost:8080/api/products/101/stock/inward
```

Body:

```json
{
  "quantity": 5
}
```

The API adds 5 to the product's current quantity and returns the updated product. If product 101 had 10 units, it now has 15. The successful change is also written to movement history as an `INWARD` movement.

## 5. Remove stock (stock outward)

When units are shipped or otherwise removed, send a `PUT` request to:

```text
http://localhost:8080/api/products/101/stock/outward
```

Body:

```json
{
  "quantity": 8
}
```

The API subtracts 8 from the current quantity and returns the updated product. If it had 15 units, it now has 7. The successful change is recorded as an `OUTWARD` movement.

The quantity must be a positive whole number. The API will not allow stock to go below zero: if there are 7 units and you request 8, it returns `409 Conflict`, and neither the quantity nor movement history changes. You may remove all available stock, which leaves the quantity at zero.

## 6. Review stock-change history

To see all successful stock changes, send:

```text
GET http://localhost:8080/api/movements
```

To see changes for one product, send:

```text
GET http://localhost:8080/api/movements/product/101
```

Each movement includes its ID, product ID, type (`INWARD` or `OUTWARD`), changed quantity, timestamp, quantity before, and quantity after. Results are newest first. Creating a product does not create a movement; movements are created by successful stock inward and outward requests.

## 7. Check low-stock alerts

Send:

```text
GET http://localhost:8080/api/alerts/low-stock
```

The response includes products where `quantity < reorderLevel`. For example, a product with 7 units and a reorder level of 12 appears in the response. A product with exactly 12 units and a reorder level of 12 does not. Alerts are calculated from current inventory each time, so an inward or outward operation can change the alert list immediately.

## 8. Complete example sequence

Use these requests in order to see the whole process with product 101:

1. `POST /api/products` with the product JSON in section 2. Expect quantity 10 and status `201`.
2. `GET /api/products/101`. Confirm the product is present with quantity 10.
3. `PUT /api/products/101/stock/inward` with `{"quantity":5}`. Confirm the returned quantity is 15.
4. `PUT /api/products/101/stock/outward` with `{"quantity":8}`. Confirm the returned quantity is 7.
5. `GET /api/movements/product/101`. Confirm there are two changes: inward from 10 to 15 and outward from 15 to 7, newest first.
6. `GET /api/alerts/low-stock`. Confirm the keyboard appears because 7 is below its reorder level of 12.
7. Try `PUT /api/products/101/stock/outward` with `{"quantity":8}`. Expect `409 Conflict`; the quantity stays at 7 and no movement is added.

For a product with a different ID, replace `101` in the URLs with that product's ID.

## 9. Common errors

The API returns an HTTP status and a JSON body with a `message` explaining expected errors.

| Status | Typical reason | What to do |
|---|---|---|
| `400 Bad Request` | Missing or invalid fields, blank name/description, nonpositive stock quantity, malformed JSON, or a decimal used where a whole number is expected. | Check the request URL and JSON body. Product quantity and reorder level may be zero; stock-change quantity must be greater than zero. |
| `404 Not Found` | The product ID has not been added. | Create the product first, then use its ID. |
| `409 Conflict` | The product ID already exists, or an outward change asks for more units than are available. | Choose a new ID for a new product, or reduce the outward quantity. |

## 10. Data lifetime

The application stores products and movements in memory. They are available while the current app process is running, but are erased when it stops or restarts. Add the products again after restarting. The project does not currently save inventory to a file or database.

