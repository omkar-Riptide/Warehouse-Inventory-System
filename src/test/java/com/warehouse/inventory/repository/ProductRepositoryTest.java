package com.warehouse.inventory.repository;

import com.warehouse.inventory.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void persistsProductsAndRejectsDuplicateIds() {
        repository.saveAndFlush(product(5));

        assertTrue(repository.existsById(5));
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> repository.saveAndFlush(product(5)));
    }

    @Test
    void retrievesPersistedProductAfterClearingThePersistenceContext() {
        repository.saveAndFlush(product(8));
        entityManager.clear();

        Product retrieved = repository.findById(8).orElseThrow();

        assertEquals("Product 8", retrieved.getName());
        assertEquals(10, retrieved.getQuantity());
    }

    @Test
    void returnsProductsInAscendingIdOrder() {
        repository.saveAll(List.of(product(9), product(2), product(4)));
        repository.flush();

        assertEquals(List.of(2, 4, 9), repository.findAllByOrderByIdAsc().stream()
                .map(Product::getId).toList());
    }

    private Product product(int id) {
        return new Product(id, "Product " + id, "Description " + id, 10, 2);
    }
}
