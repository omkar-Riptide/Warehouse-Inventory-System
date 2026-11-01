package com.warehouse.inventory.service;

import com.warehouse.inventory.exception.ProductNotFoundException;
import com.warehouse.inventory.exception.InvalidStockOperationException;
import com.warehouse.inventory.model.InventoryMovement;
import com.warehouse.inventory.model.MovementResponse;
import com.warehouse.inventory.model.MovementType;
import com.warehouse.inventory.model.Product;
import com.warehouse.inventory.repository.MovementRepository;
import com.warehouse.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MovementService {

    private final MovementRepository movementRepository;
    private final ProductRepository productRepository;

    public MovementService(MovementRepository movementRepository, ProductRepository productRepository) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
    }

    public MovementResponse recordMovement(Product product, MovementType type, int quantity,
                                           int quantityBefore, int quantityAfter) {
        InventoryMovement movement = new InventoryMovement(product, type, quantity, Instant.now(),
                quantityBefore, quantityAfter);
        return toResponse(movementRepository.save(movement));
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> getAllMovements() {
        return movementRepository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> getMovementsForProduct(int productId) {
        if (productId <= 0) {
            throw new InvalidStockOperationException("productId must be greater than zero");
        }
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        return movementRepository.findByProduct_IdOrderByIdDesc(productId).stream()
                .map(this::toResponse).toList();
    }

    private MovementResponse toResponse(InventoryMovement movement) {
        return new MovementResponse(movement.getId(), movement.getProduct().getId(), movement.getType(),
                movement.getQuantity(), movement.getTimestamp(), movement.getQuantityBefore(),
                movement.getQuantityAfter());
    }
}
