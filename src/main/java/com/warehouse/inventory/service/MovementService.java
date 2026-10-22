package com.warehouse.inventory.service;

import com.warehouse.inventory.exception.ProductNotFoundException;
import com.warehouse.inventory.exception.InvalidStockOperationException;
import com.warehouse.inventory.model.InventoryMovement;
import com.warehouse.inventory.model.MovementType;
import com.warehouse.inventory.repository.MovementRepository;
import com.warehouse.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovementService {

    private final MovementRepository movementRepository;
    private final ProductRepository productRepository;

    public MovementService(MovementRepository movementRepository, ProductRepository productRepository) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
    }

    public InventoryMovement recordMovement(int productId, MovementType type, int quantity,
                                           int quantityBefore, int quantityAfter) {
        return movementRepository.recordMovement(productId, type, quantity, quantityBefore, quantityAfter);
    }

    public List<InventoryMovement> getAllMovements() {
        return movementRepository.getAllMovements();
    }

    public List<InventoryMovement> getMovementsForProduct(int productId) {
        if (productId <= 0) {
            throw new InvalidStockOperationException("productId must be greater than zero");
        }
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        return movementRepository.getMovementsByProductId(productId);
    }
}
