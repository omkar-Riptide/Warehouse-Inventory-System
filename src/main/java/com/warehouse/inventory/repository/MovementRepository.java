package com.warehouse.inventory.repository;

import com.warehouse.inventory.model.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovementRepository extends JpaRepository<InventoryMovement, Long> {

    List<InventoryMovement> findAllByOrderByIdDesc();

    List<InventoryMovement> findByProduct_IdOrderByIdDesc(int productId);
}
