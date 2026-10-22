package com.warehouse.inventory.controller;

import com.warehouse.inventory.model.Product;
import com.warehouse.inventory.model.StockOperationRequest;
import com.warehouse.inventory.service.StockService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/{id}/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PutMapping("/inward")
    public Product stockInward(@PathVariable int id, @Valid @RequestBody StockOperationRequest request) {
        return stockService.moveStockInward(id, request.quantity());
    }

    @PutMapping("/outward")
    public Product stockOutward(@PathVariable int id, @Valid @RequestBody StockOperationRequest request) {
        return stockService.moveStockOutward(id, request.quantity());
    }
}
