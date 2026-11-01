package com.warehouse.inventory.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(nullable = false)
    @NotNull(message = "id is required")
    @Min(value = 1, message = "id must be greater than zero")
    private Integer id;

    @Column(nullable = false, length = 150)
    @NotBlank(message = "name must not be blank")
    @Size(max = 150, message = "name must be at most 150 characters")
    private String name;

    @Column(nullable = false, length = 1000)
    @NotBlank(message = "description must not be blank")
    @Size(max = 1000, message = "description must be at most 1000 characters")
    private String description;

    @Column(nullable = false)
    @NotNull(message = "quantity is required")
    @Min(value = 0, message = "quantity must be zero or greater")
    private Integer quantity;

    @Column(name = "reorder_level", nullable = false)
    @NotNull(message = "reorderLevel is required")
    @Min(value = 0, message = "reorderLevel must be zero or greater")
    private Integer reorderLevel;

    @Version
    @JsonIgnore
    private Long version;

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
