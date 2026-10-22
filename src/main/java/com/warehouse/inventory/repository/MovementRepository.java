package com.warehouse.inventory.repository;

import com.warehouse.inventory.model.InventoryMovement;
import com.warehouse.inventory.model.MovementType;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class MovementRepository {

    private final ConcurrentHashMap<Long, InventoryMovement> movements = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();

    public InventoryMovement recordMovement(int productId, MovementType type, int quantity,
                                            int quantityBefore, int quantityAfter) {
        long id = nextId.incrementAndGet();
        InventoryMovement movement = new InventoryMovement(id, productId, type, quantity,
                Instant.now(), quantityBefore, quantityAfter);
        movements.put(id, movement);
        return movement;
    }

    public List<InventoryMovement> getAllMovements() {
        return movements.values().stream()
                .sorted(Comparator.comparingLong(InventoryMovement::id).reversed())
                .toList();
    }

    public List<InventoryMovement> getMovementsByProductId(int productId) {
        return movements.values().stream()
                .filter(movement -> movement.productId() == productId)
                .sorted(Comparator.comparingLong(InventoryMovement::id).reversed())
                .toList();
    }
}
