package com.warehouse.inventory.repository;

import com.warehouse.inventory.model.Product;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ProductRepository {

    private final ConcurrentHashMap<Integer, Product> products = new ConcurrentHashMap<>();

    public boolean addProduct(Product product) {
        Product storedCopy = new Product(product);
        return products.putIfAbsent(storedCopy.getId(), storedCopy) == null;
    }

    public List<Product> getAllProducts() {
        return products.values().stream()
                .map(Product::new)
                .sorted(Comparator.comparingInt(Product::getId))
                .toList();
    }

    public Optional<Product> getProductById(int id) {
        return Optional.ofNullable(products.get(id)).map(Product::new);
    }

    public boolean existsById(int id) {
        return products.containsKey(id);
    }

    public boolean updateQuantity(int id, int expectedQuantity, int newQuantity) {
        AtomicBoolean updated = new AtomicBoolean(false);
        products.computeIfPresent(id, (key, storedProduct) -> {
            if (storedProduct.getQuantity() != expectedQuantity) {
                return storedProduct;
            }

            Product updatedProduct = new Product(storedProduct);
            updatedProduct.setQuantity(newQuantity);
            updated.set(true);
            return updatedProduct;
        });
        return updated.get();
    }
}
