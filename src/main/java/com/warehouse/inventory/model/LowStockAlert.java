package com.warehouse.inventory.model;

public record LowStockAlert(int productId, String productName, int quantity, int reorderLevel, String message) {
}
