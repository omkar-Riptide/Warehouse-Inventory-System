package com.warehouse.inventory.model;

import java.time.Instant;

public record InventoryMovement(
        long id,
        int productId,
        MovementType type,
        int quantity,
        Instant timestamp,
        int quantityBefore,
        int quantityAfter) {
}
