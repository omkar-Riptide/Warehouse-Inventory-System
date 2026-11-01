package com.warehouse.inventory.exception;

public class DuplicateProductException extends RuntimeException {

    public DuplicateProductException(int productId) {
        super("A product with ID " + productId + " already exists");
    }
}
