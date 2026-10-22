package com.warehouse.inventory.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockOperationRequest(
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be greater than zero")
        Integer quantity) {
}
