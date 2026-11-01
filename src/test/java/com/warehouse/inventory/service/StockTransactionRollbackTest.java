package com.warehouse.inventory.service;

import com.warehouse.inventory.model.MovementType;
import com.warehouse.inventory.model.Product;
import com.warehouse.inventory.repository.MovementRepository;
import com.warehouse.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class StockTransactionRollbackTest {

    @Autowired
    private ProductRepository products;

    @Autowired
    private MovementRepository movements;

    @Autowired
    private StockService stockService;

    @MockitoBean
    private MovementService movementService;

    @BeforeEach
    void prepareProduct() {
        movements.deleteAllInBatch();
        products.deleteAllInBatch();
        products.saveAndFlush(new Product(501, "Rollback test", "Transaction check", 10, 2));
        doThrow(new IllegalStateException("simulated movement persistence failure"))
                .when(movementService).recordMovement(any(Product.class), eq(MovementType.INWARD),
                        eq(2), eq(10), eq(12));
    }

    @Test
    void rollsBackProductQuantityWhenMovementRecordingFails() {
        assertThrows(IllegalStateException.class, () -> stockService.moveStockInward(501, 2));

        assertEquals(10, products.findById(501).orElseThrow().getQuantity());
        assertEquals(0, movements.count());
    }
}
