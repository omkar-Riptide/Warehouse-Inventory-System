package com.warehouse.inventory.service;

import com.warehouse.inventory.model.LowStockAlert;
import com.warehouse.inventory.model.Product;
import com.warehouse.inventory.repository.MovementRepository;
import com.warehouse.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertServiceTest {

    private ProductRepository products;
    private AlertService alertService;
    private StockService stockService;

    @BeforeEach
    void setUp() {
        products = new ProductRepository();
        alertService = new AlertService(products);
        MovementService movementService = new MovementService(new MovementRepository(), products);
        stockService = new StockService(products, movementService);
    }

    @Test
    void includesOnlyProductsBelowTheirReorderLevel() {
        products.addProduct(product(1, 3, 5));
        products.addProduct(product(2, 5, 5));
        products.addProduct(product(3, 8, 5));
        products.addProduct(product(4, 1, 10));

        List<LowStockAlert> alerts = alertService.getLowStockAlerts();

        assertEquals(List.of(1, 4), alerts.stream().map(LowStockAlert::productId).toList());
        assertEquals(3, alerts.getFirst().quantity());
        assertEquals(5, alerts.getFirst().reorderLevel());
        assertTrue(alerts.getFirst().message().contains("Product 1"));
    }

    @Test
    void returnsEmptyForEmptyInventoryAndWhenNoProductIsLow() {
        assertTrue(alertService.getLowStockAlerts().isEmpty());
        products.addProduct(product(1, 5, 5));
        products.addProduct(product(2, 8, 5));
        assertTrue(alertService.getLowStockAlerts().isEmpty());
    }

    @Test
    void alertsReflectSuccessfulAndRejectedStockOperationsImmediately() {
        products.addProduct(product(1, 5, 10));

        assertEquals(1, alertService.getLowStockAlerts().size());
        stockService.moveStockInward(1, 5);
        assertTrue(alertService.getLowStockAlerts().isEmpty());
        stockService.moveStockOutward(1, 2);
        assertEquals(1, alertService.getLowStockAlerts().size());
    }

    private Product product(int id, int quantity, int reorderLevel) {
        return new Product(id, "Product " + id, "Description " + id, quantity, reorderLevel);
    }
}
