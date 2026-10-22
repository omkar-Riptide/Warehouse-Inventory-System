package com.warehouse.inventory.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class Product {

    @NotNull(message = "id is required")
    @Min(value = 1, message = "id must be greater than zero")
    private Integer id;

    @NotBlank(message = "name must not be blank")
    private String name;

    @NotBlank(message = "description must not be blank")
    private String description;

    @NotNull(message = "quantity is required")
    @Min(value = 0, message = "quantity must be zero or greater")
    private Integer quantity;

    @NotNull(message = "reorderLevel is required")
    @Min(value = 0, message = "reorderLevel must be zero or greater")
    private Integer reorderLevel;

    public Product() {
    }

    public Product(int id, String name, String description, int quantity, int reorderLevel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
    }

    public Product(Product other) {
        this(other.id, other.name, other.description, other.quantity, other.reorderLevel);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }
}
