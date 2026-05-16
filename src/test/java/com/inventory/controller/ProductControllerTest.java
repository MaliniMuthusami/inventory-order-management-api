package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.AuthRequest;
import com.inventory.dto.request.CategoryRequest;
import com.inventory.dto.request.ProductRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MongoTemplate mongoTemplate;

    private String adminToken;
    private String userToken;
    private String categoryId;

    @BeforeEach
    void setup() throws Exception {
        adminToken  = registerAndGetToken("admin1", "admin@test.com", "pass1234", "ADMIN");
        userToken   = registerAndGetToken("user1",  "user@test.com",  "pass1234", "USER");
        categoryId  = createCategory("Electronics");
    }

    @AfterEach
    void cleanup() { mongoTemplate.getDb().drop(); }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    void createProduct_admin_success() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildCreate("Laptop", "Gaming laptop", 999.99, 50, List.of(categoryId)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(999.99))
                .andExpect(jsonPath("$.stockQuantity").value(50))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.categories[0].name").value("Electronics"));
    }

    @Test
    void createProduct_user_forbidden() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildCreate("Phone", "Smartphone", 499.99, 20, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createProduct_missingName_badRequest() throws Exception {
        ProductRequest.Create req = new ProductRequest.Create();
        req.setPrice(10.00);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void createProduct_invalidCategoryId_notFound() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                buildCreate("Widget", "A widget", 5.00, 100, List.of("nonexistent-id")))))
                .andExpect(status().isNotFound());
    }

    // ── Read / Browse ─────────────────────────────────────────────────────────

    @Test
    void getProductById_success() throws Exception {
        String productId = createProduct("Mouse", 29.99, 100);

        mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mouse"));
    }

    @Test
    void getAllProducts_paginated() throws Exception {
        for (int i = 1; i <= 5; i++) {
            createProduct("Product " + i, (double)(i * 10), i * 10);
        }

        mockMvc.perform(get("/api/products?page=0&size=3")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getAllProducts_sortByPriceDesc() throws Exception {
        createProduct("Cheap", 10.00, 10);
        createProduct("Mid",   50.00, 10);
        createProduct("Pricey", 200.00, 10);

        mockMvc.perform(get("/api/products?sortBy=price&direction=desc")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Pricey"))
                .andExpect(jsonPath("$.content[2].name").value("Cheap"));
    }

    @Test
    void searchByName_returnsMatchingProducts() throws Exception {
        createProduct("Wireless Keyboard", 49.99, 30);
        createProduct("Wireless Mouse",    29.99, 50);
        createProduct("USB Hub",           19.99, 20);

        mockMvc.perform(get("/api/products/search?name=wireless")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void filterByCategory_returnsOnlyCategoryProducts() throws Exception {
        String otherCategoryId = createCategory("Furniture");
        createProduct("Keyboard",  59.99, 40);             // Electronics
        createProductInCategory("Chair", 150.00, 10, otherCategoryId); // Furniture

        mockMvc.perform(get("/api/products/category/" + categoryId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Keyboard"));
    }

    @Test
    void getLowStock_returnsProductsBelowThreshold() throws Exception {
        createProductWithStock("High Stock",  10.00, 200);
        createProductWithStock("Low Stock A", 20.00, 5);
        createProductWithStock("Low Stock B", 30.00, 3);

        mockMvc.perform(get("/api/products/low-stock?threshold=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Test
    void updateProduct_admin_success() throws Exception {
        String productId = createProduct("Old Name", 10.00, 50);

        ProductRequest.Update update = new ProductRequest.Update();
        update.setName("New Name");
        update.setPrice(15.00);
        update.setActive(false);

        mockMvc.perform(put("/api/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.price").value(15.00))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void updateProduct_user_forbidden() throws Exception {
        String productId = createProduct("Item", 10.00, 10);
        ProductRequest.Update update = new ProductRequest.Update();
        update.setName("Hacked Name");

        mockMvc.perform(put("/api/products/" + productId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    void deleteProduct_admin_success() throws Exception {
        String productId = createProduct("ToDelete", 5.00, 10);

        mockMvc.perform(delete("/api/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String createProduct(String name, Double price, int stock) throws Exception {
        return createProductInCategory(name, price, stock, categoryId);
    }

    private String createProductWithStock(String name, Double price, int stock) throws Exception {
        return createProductInCategory(name, price, stock, null);
    }

    private String createProductInCategory(String name, Double price, int stock, String catId) throws Exception {
        ProductRequest.Create req = buildCreate(name, "desc", price, stock,
                catId != null ? List.of(catId) : List.of());
        MvcResult r = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    private ProductRequest.Create buildCreate(String name, String desc, Double price,
                                              int stock, List<String> catIds) {
        ProductRequest.Create r = new ProductRequest.Create();
        r.setName(name);
        r.setDescription(desc);
        r.setPrice(price);
        r.setStockQuantity(stock);
        if (catIds != null) r.setCategoryIds(catIds);
        return r;
    }

    private String createCategory(String name) throws Exception {
        CategoryRequest req = new CategoryRequest();
        req.setName(name);
        MvcResult r = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    private String registerAndGetToken(String username, String email, String password, String role) throws Exception {
        AuthRequest.Register reg = new AuthRequest.Register();
        reg.setUsername(username);
        reg.setEmail(email);
        reg.setPassword(password);
        MvcResult r = mockMvc.perform(post("/api/auth/register?role=" + role)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }
}
