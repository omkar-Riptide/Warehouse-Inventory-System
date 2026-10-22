package com.warehouse.inventory;

import com.warehouse.inventory.model.Product;
import com.warehouse.inventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WarehouseApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void productStockMovementsAndAlertsStayInSyncAcrossTheApi() throws Exception {
        mockMvc.perform(get("/api/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(201, 10, 12)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(10));

        mockMvc.perform(get("/api/movements/product/201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(get("/api/products/201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(10));

        mockMvc.perform(put("/api/products/201/stock/inward").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(15));

        mockMvc.perform(put("/api/products/201/stock/outward").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(7));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantity").value(7));

        mockMvc.perform(get("/api/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].type").value("OUTWARD"))
                .andExpect(jsonPath("$[0].quantity").value(8))
                .andExpect(jsonPath("$[0].quantityBefore").value(15))
                .andExpect(jsonPath("$[0].quantityAfter").value(7))
                .andExpect(jsonPath("$[0].timestamp").exists())
                .andExpect(jsonPath("$[1].type").value("INWARD"))
                .andExpect(jsonPath("$[1].quantityBefore").value(10))
                .andExpect(jsonPath("$[1].quantityAfter").value(15));

        mockMvc.perform(get("/api/movements/product/201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].productId").value(201));

        mockMvc.perform(get("/api/alerts/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(201))
                .andExpect(jsonPath("$[0].quantity").value(7))
                .andExpect(jsonPath("$[0].reorderLevel").value(12));

        mockMvc.perform(put("/api/products/201/stock/outward").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":8}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/products/201/stock/inward").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/products/201/stock/inward").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":-2}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/products/201/stock/inward").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1.5}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/products/201/stock/inward").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/products/201/stock/inward").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/products/201/stock/inward").contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/products/999/stock/inward").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/movements/product/999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/products/201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(7));
        mockMvc.perform(get("/api/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));

        productRepository.addProduct(new Product(202, "Maximum", "At integer limit", Integer.MAX_VALUE, 0));
        mockMvc.perform(put("/api/products/202/stock/inward").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":1}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));

        mockMvc.perform(get("/api/movements/product/202"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/alerts/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }

    private String productJson(int id, int quantity, int reorderLevel) {
        return "{\"id\":" + id + ",\"name\":\"Keyboard\",\"description\":\"Wireless keyboard\","
                + "\"quantity\":" + quantity + ",\"reorderLevel\":" + reorderLevel + "}";
    }
}
