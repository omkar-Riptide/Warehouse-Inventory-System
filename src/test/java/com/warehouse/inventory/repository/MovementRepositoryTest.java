package com.warehouse.inventory.repository;

import com.warehouse.inventory.model.InventoryMovement;
import com.warehouse.inventory.model.MovementType;
import com.warehouse.inventory.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class MovementRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MovementRepository movementRepository;

    @Test
    void persistsMovementsAndReturnsHistoryNewestFirstByProduct() {
        Product product = productRepository.saveAndFlush(
                new Product(11, "Tape", "Packing tape", 10, 2));
        movementRepository.saveAll(List.of(
                movement(product, MovementType.INWARD, 2, 10, 12),
                movement(product, MovementType.OUTWARD, 1, 12, 11)));
        movementRepository.flush();

        List<InventoryMovement> history = movementRepository.findByProduct_IdOrderByIdDesc(11);

        assertEquals(2, history.size());
        org.junit.jupiter.api.Assertions.assertTrue(history.get(0).getId() > history.get(1).getId());
        assertEquals(MovementType.OUTWARD, history.get(0).getType());
        assertEquals(12, history.get(0).getQuantityBefore());
        assertEquals(11, history.get(0).getQuantityAfter());
        assertEquals(history.get(0).getId(), movementRepository.findAllByOrderByIdDesc().get(0).getId());
    }

    private InventoryMovement movement(Product product, MovementType type, int quantity,
                                       int before, int after) {
        return new InventoryMovement(product, type, quantity, Instant.now(), before, after);
    }
}
