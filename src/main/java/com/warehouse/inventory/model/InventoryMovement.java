package com.warehouse.inventory.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 10)
    private MovementType type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "movement_timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "previous_quantity", nullable = false)
    private Integer quantityBefore;

    @Column(name = "updated_quantity", nullable = false)
    private Integer quantityAfter;

    protected InventoryMovement() {
    }

    public InventoryMovement(Product product, MovementType type, int quantity, Instant timestamp,
                             int quantityBefore, int quantityAfter) {
        this.product = product;
        this.type = type;
        this.quantity = quantity;
        this.timestamp = timestamp;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public MovementType getType() {
        return type;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Integer getQuantityBefore() {
        return quantityBefore;
    }

    public Integer getQuantityAfter() {
        return quantityAfter;
    }
}
