package com.warehouse.inventory.repository;

import com.warehouse.inventory.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductRepositoryTest {

    private ProductRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ProductRepository();
    }

    @Test
    void addsProductAndRejectsDuplicateId() {
        Product product = product(5);

        assertTrue(repository.addProduct(product));
        assertFalse(repository.addProduct(product(5)));
        assertTrue(repository.existsById(5));
    }

    @Test
    void retrievesExistingProductAsDefensiveCopy() {
        Product original = product(8);
        repository.addProduct(original);

        Product retrieved = repository.getProductById(8).orElseThrow();
        assertNotSame(original, retrieved);
        retrieved.setName("Changed outside repository");

        assertEquals("Product 8", repository.getProductById(8).orElseThrow().getName());
    }

    @Test
    void returnsEmptyForUnknownId() {
        assertTrue(repository.getProductById(404).isEmpty());
    }

    @Test
    void returnsProductsInAscendingIdOrderAndSnapshotList() {
        repository.addProduct(product(9));
        repository.addProduct(product(2));
        List<Product> snapshot = repository.getAllProducts();

        repository.addProduct(product(4));
        assertEquals(List.of(2, 9), snapshot.stream().map(Product::getId).toList());
        assertEquals(List.of(2, 4, 9), repository.getAllProducts().stream().map(Product::getId).toList());
        snapshot.get(0).setName("Changed snapshot product");
        assertEquals("Product 2", repository.getProductById(2).orElseThrow().getName());
    }

    private Product product(int id) {
        return new Product(id, "Product " + id, "Description " + id, 10, 2);
    }
}
