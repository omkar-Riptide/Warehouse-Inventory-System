package com.warehouse.inventory.service;

import com.warehouse.inventory.model.LowStockAlert;
import com.warehouse.inventory.model.Product;
import com.warehouse.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(AlertService.class)
class AlertServiceTest {

    @Autowired
    private ProductRepository products;

    @Autowired
    private AlertService alertService;

    @Test
    void includesOnlyProductsBelowTheirReorderLevel() {
        products.saveAllAndFlush(List.of(
                product(1, 3, 5), product(2, 5, 5), product(3, 8, 5), product(4, 1, 10)));

        List<LowStockAlert> alerts = alertService.getLowStockAlerts();

        assertEquals(List.of(1, 4), alerts.stream().map(LowStockAlert::productId).toList());
        assertEquals(3, alerts.getFirst().quantity());
        assertEquals(5, alerts.getFirst().reorderLevel());
        assertTrue(alerts.getFirst().message().contains("Product 1"));
    }

    @Test
    void returnsEmptyForEmptyInventoryAndWhenNoProductIsLow() {
        assertTrue(alertService.getLowStockAlerts().isEmpty());
        products.saveAllAndFlush(List.of(product(1, 5, 5), product(2, 8, 5)));
        assertTrue(alertService.getLowStockAlerts().isEmpty());
    }

    private Product product(int id, int quantity, int reorderLevel) {
        return new Product(id, "Product " + id, "Description " + id, quantity, reorderLevel);
    }
}
