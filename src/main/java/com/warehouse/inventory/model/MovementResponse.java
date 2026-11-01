package com.warehouse.inventory.model;

import java.time.Instant;

public record MovementResponse(
        long id,
        int productId,
        MovementType type,
        int quantity,
        Instant timestamp,
        int quantityBefore,
        int quantityAfter) {
}
