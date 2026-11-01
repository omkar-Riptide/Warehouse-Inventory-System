package com.warehouse.inventory.service;

import com.warehouse.inventory.exception.DuplicateProductException;
import com.warehouse.inventory.exception.ProductNotFoundException;
import com.warehouse.inventory.model.Product;
import com.warehouse.inventory.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product createProduct(Product product) {
        if (productRepository.existsById(product.getId())) {
            throw new DuplicateProductException(product.getId());
        }
        try {
            return productRepository.saveAndFlush(product);
        } catch (DataIntegrityViolationException exception) {
            if (productRepository.existsById(product.getId())) {
                throw new DuplicateProductException(product.getId());
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Product getProductById(int id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }
}
