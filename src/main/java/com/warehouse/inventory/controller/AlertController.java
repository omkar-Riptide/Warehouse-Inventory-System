package com.warehouse.inventory.controller;

import com.warehouse.inventory.model.LowStockAlert;
import com.warehouse.inventory.service.AlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping("/low-stock")
    public List<LowStockAlert> getLowStockAlerts() {
        return alertService.getLowStockAlerts();
    }
}
