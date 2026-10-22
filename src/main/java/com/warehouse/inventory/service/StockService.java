package com.warehouse.inventory.service;

import com.warehouse.inventory.exception.InsufficientStockException;
import com.warehouse.inventory.exception.InvalidStockOperationException;
import com.warehouse.inventory.exception.ProductNotFoundException;
import com.warehouse.inventory.model.InventoryMovement;
import com.warehouse.inventory.model.MovementType;
import com.warehouse.inventory.model.Product;
import com.warehouse.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class StockService {

    private final ProductRepository productRepository;
    private final MovementService movementService;

    public StockService(ProductRepository productRepository, MovementService movementService) {
        this.productRepository = productRepository;
        this.movementService = movementService;
    }

    public synchronized Product moveStockInward(int productId, int quantity) {
        return changeStock(productId, quantity, MovementType.INWARD);
    }

    public synchronized Product moveStockOutward(int productId, int quantity) {
        return changeStock(productId, quantity, MovementType.OUTWARD);
    }

    private Product changeStock(int productId, int quantity, MovementType type) {
        if (productId <= 0) {
            throw new InvalidStockOperationException("id must be greater than zero");
        }
        if (quantity <= 0) {
            throw new InvalidStockOperationException("quantity must be greater than zero");
        }

        Product current = productRepository.getProductById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        int before = current.getQuantity();
        int after;

        if (type == MovementType.INWARD) {
            if (quantity > Integer.MAX_VALUE - before) {
                throw new InvalidStockOperationException("stock quantity would exceed the maximum integer value");
            }
            after = before + quantity;
        } else {
            if (quantity > before) {
                throw new InsufficientStockException(productId, before, quantity);
            }
            after = before - quantity;
        }

        if (!productRepository.updateQuantity(productId, before, after)) {
            throw new IllegalStateException("Product stock changed unexpectedly during the operation");
        }

        try {
            movementService.recordMovement(productId, type, quantity, before, after);
        } catch (RuntimeException exception) {
            boolean rolledBack = productRepository.updateQuantity(productId, after, before);
            if (!rolledBack) {
                throw new IllegalStateException("Movement recording failed and stock rollback was unsuccessful", exception);
            }
            throw exception;
        }

        return productRepository.getProductById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
