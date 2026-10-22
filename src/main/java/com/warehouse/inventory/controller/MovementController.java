package com.warehouse.inventory.controller;

import com.warehouse.inventory.model.InventoryMovement;
import com.warehouse.inventory.service.MovementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movements")
public class MovementController {

    private final MovementService movementService;

    public MovementController(MovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping
    public List<InventoryMovement> getAllMovements() {
        return movementService.getAllMovements();
    }

    @GetMapping("/product/{productId}")
    public List<InventoryMovement> getMovementsForProduct(@PathVariable int productId) {
        return movementService.getMovementsForProduct(productId);
    }
}
