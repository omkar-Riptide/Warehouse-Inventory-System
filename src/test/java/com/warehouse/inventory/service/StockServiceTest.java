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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StockServiceTest {

    private ProductRepository products;
    private MovementRepository movements;
    private MovementService movementService;
    private StockService stockService;

    @BeforeEach
    void setUp() {
        products = new ProductRepository();
        movements = new MovementRepository();
        movementService = new MovementService(movements, products);
        stockService = new StockService(products, movementService);
    }

    @Test
    void inwardUpdatesProductAndRecordsCompleteMovement() {
        products.addProduct(product(1, 10, 5));

        Product updated = stockService.moveStockInward(1, 4);
        InventoryMovement movement = movements.getAllMovements().getFirst();

        assertEquals(14, updated.getQuantity());
        assertEquals(MovementType.INWARD, movement.type());
        assertEquals(4, movement.quantity());
        assertEquals(10, movement.quantityBefore());
        assertEquals(14, movement.quantityAfter());
        assertEquals(1, movement.productId());
        assertNotNull(movement.timestamp());
        assertEquals(1, movement.id());
    }

    @Test
    void outwardCanRemoveAllAvailableStockAndRecordsMovement() {
        products.addProduct(product(1, 7, 5));

        Product updated = stockService.moveStockOutward(1, 7);
        InventoryMovement movement = movements.getAllMovements().getFirst();

        assertEquals(0, updated.getQuantity());
        assertEquals(MovementType.OUTWARD, movement.type());
        assertEquals(7, movement.quantity());
        assertEquals(7, movement.quantityBefore());
        assertEquals(0, movement.quantityAfter());
    }

    @Test
    void unknownProductAndInvalidQuantitiesDoNotCreateMovements() {
        products.addProduct(product(1, 10, 5));

        assertThrows(ProductNotFoundException.class, () -> stockService.moveStockInward(99, 1));
        assertThrows(InvalidStockOperationException.class, () -> stockService.moveStockInward(1, 0));
        assertThrows(InvalidStockOperationException.class, () -> stockService.moveStockOutward(1, -1));
        assertThrows(InvalidStockOperationException.class, () -> stockService.moveStockInward(0, 1));

        assertTrue(movements.getAllMovements().isEmpty());
        assertEquals(10, products.getProductById(1).orElseThrow().getQuantity());
    }

    @Test
    void rejectsInsufficientStockAndIntegerOverflowWithoutChangingState() {
        products.addProduct(product(1, 10, 5));
        products.addProduct(product(2, Integer.MAX_VALUE, 5));

        assertThrows(InsufficientStockException.class, () -> stockService.moveStockOutward(1, 11));
        assertThrows(InvalidStockOperationException.class, () -> stockService.moveStockInward(2, 1));

        assertEquals(10, products.getProductById(1).orElseThrow().getQuantity());
        assertEquals(Integer.MAX_VALUE, products.getProductById(2).orElseThrow().getQuantity());
        assertTrue(movements.getAllMovements().isEmpty());
    }

    @Test
    void rollsBackQuantityWhenMovementRecordingFails() {
        products.addProduct(product(1, 10, 5));
        MovementService failingMovementService = new MovementService(movements, products) {
            @Override
            public InventoryMovement recordMovement(int productId, MovementType type, int quantity,
                                                    int quantityBefore, int quantityAfter) {
                throw new IllegalStateException("simulated recording failure");
            }
        };
        StockService transactionalService = new StockService(products, failingMovementService);

        assertThrows(IllegalStateException.class, () -> transactionalService.moveStockInward(1, 2));

        assertEquals(10, products.getProductById(1).orElseThrow().getQuantity());
        assertTrue(movements.getAllMovements().isEmpty());
    }

    @Test
    void doesNotRecordMovementWhenStockUpdateIsRejected() {
        ProductRepository rejectingRepository = new ProductRepository() {
            @Override
            public boolean updateQuantity(int id, int expectedQuantity, int newQuantity) {
                return false;
            }
        };
        rejectingRepository.addProduct(product(1, 10, 5));
        MovementRepository untouchedMovements = new MovementRepository();
        StockService rejectingService = new StockService(rejectingRepository,
                new MovementService(untouchedMovements, rejectingRepository));

        assertThrows(IllegalStateException.class, () -> rejectingService.moveStockInward(1, 1));

        assertEquals(10, rejectingRepository.getProductById(1).orElseThrow().getQuantity());
        assertTrue(untouchedMovements.getAllMovements().isEmpty());
    }

    @Test
    void competingOutwardOperationsCannotMakeStockNegative() throws Exception {
        products.addProduct(product(1, 10, 5));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> tryOutward(start, 1, 7));
            Future<Boolean> second = executor.submit(() -> tryOutward(start, 1, 7));
            start.countDown();

            int successes = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
            assertEquals(1, successes);
            assertEquals(3, products.getProductById(1).orElseThrow().getQuantity());
            assertEquals(1, movements.getAllMovements().size());
            assertTrue(movements.getAllMovements().stream().allMatch(m -> m.quantityAfter() >= 0));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void movementHistoryIsNewestFirstAndProductFiltered() {
        products.addProduct(product(1, 10, 5));
        products.addProduct(product(2, 2, 5));
        stockService.moveStockInward(1, 1);
        stockService.moveStockOutward(2, 1);

        List<InventoryMovement> all = movementService.getAllMovements();
        assertEquals(List.of(2L, 1L), all.stream().map(InventoryMovement::id).toList());
        assertEquals(List.of(2), movementService.getMovementsForProduct(2).stream()
                .map(InventoryMovement::productId).toList());
        assertTrue(movementService.getMovementsForProduct(1).stream()
                .allMatch(movement -> movement.productId() == 1));
        assertThrows(ProductNotFoundException.class, () -> movementService.getMovementsForProduct(404));
        assertTrue(movementService.getMovementsForProduct(1).getFirst().timestamp() != null);
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
