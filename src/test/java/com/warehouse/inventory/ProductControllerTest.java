package com.warehouse.inventory;

import com.warehouse.inventory.controller.ApiExceptionHandler;
import com.warehouse.inventory.model.Product;
import com.warehouse.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import com.warehouse.inventory.controller.ProductController;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(ApiExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        when(productRepository.getAllProducts()).thenReturn(List.of());
        when(productRepository.getProductById(any(Integer.class))).thenReturn(Optional.empty());
        when(productRepository.addProduct(any(Product.class))).thenReturn(true);
    }

    @Test
    void createsValidProductWithCreatedStatus() throws Exception {
        when(productRepository.getProductById(101)).thenReturn(Optional.of(product(101)));

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(101)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.reorderLevel").value(10));
    }

    @Test
    void listsProductsAndReturnsEmptyArrayWhenRepositoryIsEmpty() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listsExistingProducts() throws Exception {
        when(productRepository.getAllProducts()).thenReturn(List.of(product(101)));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].name").value("Keyboard"));
    }

    @Test
    void retrievesExistingProduct() throws Exception {
        when(productRepository.getProductById(101)).thenReturn(Optional.of(product(101)));

        mockMvc.perform(get("/api/products/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101));
    }

    @Test
    void unknownProductReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/products/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product with ID 404 was not found"));
    }

    @Test
    void duplicateProductReturnsConflict() throws Exception {
        when(productRepository.addProduct(any(Product.class))).thenReturn(false);

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(101)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A product with ID 101 already exists"));
    }

    @Test
    void missingOrBlankNameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"description\":\"Keyboard\",\"quantity\":2,\"reorderLevel\":1}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(1).replace("Keyboard", "   ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingOrBlankDescriptionReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"name\":\"Keyboard\",\"quantity\":2,\"reorderLevel\":1}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(1).replace("Wireless keyboard", "  ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsZeroAndNegativeProductIds() throws Exception {
        for (int id : new int[]{0, -1}) {
            mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                            .content(validJson(id)))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(get("/api/products/0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("id must be an integer"));
    }

    @Test
    void rejectsNegativeQuantityAndReorderLevel() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(1).replace("\"quantity\":50", "\"quantity\":-1")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(validJson(1).replace("\"reorderLevel\":10", "\"reorderLevel\":-1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    private Product product(int id) {
        return new Product(id, "Keyboard", "Wireless keyboard", 50, 10);
    }

    private String validJson(int id) {
        return "{\"id\":" + id + ",\"name\":\"Keyboard\",\"description\":\"Wireless keyboard\","
                + "\"quantity\":50,\"reorderLevel\":10}";
    }
}
