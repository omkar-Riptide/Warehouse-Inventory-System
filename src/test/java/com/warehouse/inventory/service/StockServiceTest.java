package com.warehouse.inventory.service;

import com.warehouse.inventory.exception.InsufficientStockException;
import com.warehouse.inventory.exception.InvalidStockOperationException;
import com.warehouse.inventory.exception.ProductNotFoundException;
import com.warehouse.inventory.model.InventoryMovement;
import com.warehouse.inventory.model.MovementType;
import com.warehouse.inventory.model.Product;
import com.warehouse.inventory.repository.MovementRepository;
import com.warehouse.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class StockServiceTest {

    @Autowired
    private ProductRepository products;

    @Autowired
    private MovementRepository movements;

    @Autowired
    private StockService stockService;

    @Autowired
    private MovementService movementService;

    @BeforeEach
    void clearDatabase() {
        movements.deleteAllInBatch();
        products.deleteAllInBatch();
    }

    @Test
    void inwardUpdatesProductAndRecordsCompleteMovement() {
        products.saveAndFlush(product(1, 10, 5));

        Product updated = stockService.moveStockInward(1, 4);
        InventoryMovement movement = movements.findAllByOrderByIdDesc().getFirst();

        assertEquals(14, updated.getQuantity());
        assertEquals(MovementType.INWARD, movement.getType());
        assertEquals(4, movement.getQuantity());
        assertEquals(10, movement.getQuantityBefore());
        assertEquals(14, movement.getQuantityAfter());
        assertEquals(1, movement.getProduct().getId());
        assertNotNull(movement.getTimestamp());
        assertNotNull(movement.getId());
    }

    @Test
    void outwardCanRemoveAllAvailableStockAndRecordsMovement() {
        products.saveAndFlush(product(1, 7, 5));

        Product updated = stockService.moveStockOutward(1, 7);
        InventoryMovement movement = movements.findAllByOrderByIdDesc().getFirst();

        assertEquals(0, updated.getQuantity());
        assertEquals(MovementType.OUTWARD, movement.getType());
        assertEquals(7, movement.getQuantity());
        assertEquals(7, movement.getQuantityBefore());
        assertEquals(0, movement.getQuantityAfter());
    }

    @Test
    void unknownProductAndInvalidQuantitiesDoNotCreateMovements() {
        products.saveAndFlush(product(1, 10, 5));

        assertThrows(ProductNotFoundException.class, () -> stockService.moveStockInward(99, 1));
        assertThrows(InvalidStockOperationException.class, () -> stockService.moveStockInward(1, 0));
        assertThrows(InvalidStockOperationException.class, () -> stockService.moveStockOutward(1, -1));
        assertThrows(InvalidStockOperationException.class, () -> stockService.moveStockInward(0, 1));

        assertTrue(movements.findAll().isEmpty());
        assertEquals(10, products.findById(1).orElseThrow().getQuantity());
    }

    @Test
    void rejectsInsufficientStockAndIntegerOverflowWithoutChangingState() {
        products.saveAllAndFlush(List.of(
                product(1, 10, 5), product(2, Integer.MAX_VALUE, 5)));

        assertThrows(InsufficientStockException.class, () -> stockService.moveStockOutward(1, 11));
        assertThrows(InvalidStockOperationException.class, () -> stockService.moveStockInward(2, 1));

        assertEquals(10, products.findById(1).orElseThrow().getQuantity());
        assertEquals(Integer.MAX_VALUE, products.findById(2).orElseThrow().getQuantity());
        assertTrue(movements.findAll().isEmpty());
    }

    @Test
    void competingOutwardOperationsCannotMakeStockNegative() throws Exception {
        products.saveAndFlush(product(1, 10, 5));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> tryOutward(start, 1, 7));
            Future<Boolean> second = executor.submit(() -> tryOutward(start, 1, 7));
            start.countDown();

            int successes = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(3, products.findById(1).orElseThrow().getQuantity());
            assertEquals(1, movements.findAll().size());
            assertTrue(movements.findAll().stream().allMatch(m -> m.getQuantityAfter() >= 0));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void movementHistoryIsNewestFirstAndProductFiltered() {
        products.saveAllAndFlush(List.of(product(1, 10, 5), product(2, 2, 5)));
        stockService.moveStockInward(1, 1);
        stockService.moveStockOutward(2, 1);

        List<com.warehouse.inventory.model.MovementResponse> all = movementService.getAllMovements();
        assertTrue(all.get(0).id() > all.get(1).id());
        assertEquals(MovementType.OUTWARD, all.get(0).type());
        assertEquals(MovementType.INWARD, all.get(1).type());
        assertEquals(List.of(2), movementService.getMovementsForProduct(2).stream()
                .map(com.warehouse.inventory.model.MovementResponse::productId).toList());
        assertTrue(movementService.getMovementsForProduct(1).stream()
                .allMatch(movement -> movement.productId() == 1));
        assertThrows(ProductNotFoundException.class, () -> movementService.getMovementsForProduct(404));
        assertNotNull(movementService.getMovementsForProduct(1).getFirst().timestamp());
    }

    private boolean tryOutward(CountDownLatch start, int productId, int quantity) throws InterruptedException {
        start.await();
        try {
            stockService.moveStockOutward(productId, quantity);
            return true;
        } catch (InsufficientStockException exception) {
            return false;
        }
    }

    private Product product(int id, int quantity, int reorderLevel) {
        return new Product(id, "Product " + id, "Description " + id, quantity, reorderLevel);
    }
}
