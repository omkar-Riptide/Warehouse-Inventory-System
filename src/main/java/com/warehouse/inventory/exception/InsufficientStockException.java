package com.warehouse.inventory.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(int productId, int available, int requested) {
        super("Product " + productId + " has " + available + " available; cannot remove " + requested);
    }
}
