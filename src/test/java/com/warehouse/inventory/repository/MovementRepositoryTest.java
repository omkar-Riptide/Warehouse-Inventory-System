package com.warehouse.inventory.repository;

import com.warehouse.inventory.model.InventoryMovement;
import com.warehouse.inventory.model.MovementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovementRepositoryTest {

    private MovementRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MovementRepository();
    }

    @Test
    void historyIsAReadOnlySnapshotAndMovementIdsAreUnique() {
        InventoryMovement first = repository.recordMovement(1, MovementType.INWARD, 3, 2, 5);
        List<InventoryMovement> snapshot = repository.getAllMovements();
        InventoryMovement second = repository.recordMovement(1, MovementType.OUTWARD, 1, 5, 4);

        assertNotEquals(first.id(), second.id());
        assertEquals(1, snapshot.size());
        assertEquals(first, snapshot.getFirst());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(second));
    }
}
