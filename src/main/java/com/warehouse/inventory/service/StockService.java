package com.warehouse.inventory.service;

import com.warehouse.inventory.exception.InsufficientStockException;
import com.warehouse.inventory.exception.InvalidStockOperationException;
import com.warehouse.inventory.exception.ProductNotFoundException;
import com.warehouse.inventory.model.MovementType;
import com.warehouse.inventory.model.Product;
import com.warehouse.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final ProductRepository productRepository;
    private final MovementService movementService;

    public StockService(ProductRepository productRepository, MovementService movementService) {
        this.productRepository = productRepository;
        this.movementService = movementService;
    }

    @Transactional
    public Product moveStockInward(int productId, int quantity) {
        return changeStock(productId, quantity, MovementType.INWARD);
    }

    @Transactional
    public Product moveStockOutward(int productId, int quantity) {
        return changeStock(productId, quantity, MovementType.OUTWARD);
    }

    private Product changeStock(int productId, int quantity, MovementType type) {
        if (productId <= 0) {
            throw new InvalidStockOperationException("id must be greater than zero");
        }
        if (quantity <= 0) {
            throw new InvalidStockOperationException("quantity must be greater than zero");
        }

        Product current = productRepository.findByIdForUpdate(productId)
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

        current.setQuantity(after);
        movementService.recordMovement(current, type, quantity, before, after);
        return current;
    }
}
