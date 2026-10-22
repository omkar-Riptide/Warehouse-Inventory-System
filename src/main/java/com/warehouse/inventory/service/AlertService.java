package com.warehouse.inventory.service;

import com.warehouse.inventory.model.LowStockAlert;
import com.warehouse.inventory.model.Product;
import com.warehouse.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertService {

    private final ProductRepository productRepository;

    public AlertService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<LowStockAlert> getLowStockAlerts() {
        return productRepository.getAllProducts().stream()
                .filter(product -> product.getQuantity() < product.getReorderLevel())
                .map(this::toAlert)
                .toList();
    }

    private LowStockAlert toAlert(Product product) {
        String message = "Low stock: " + product.getName() + " has " + product.getQuantity()
                + " units; reorder level is " + product.getReorderLevel();
        return new LowStockAlert(product.getId(), product.getName(), product.getQuantity(),
                product.getReorderLevel(), message);
    }
}
