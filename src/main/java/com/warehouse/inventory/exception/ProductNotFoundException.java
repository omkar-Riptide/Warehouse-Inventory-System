package com.warehouse.inventory.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(int productId) {
        super("Product with ID " + productId + " was not found");
    }
}
