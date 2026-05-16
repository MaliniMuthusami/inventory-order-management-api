package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.AuthRequest;
import com.inventory.dto.request.OrderRequest;
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
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MongoTemplate mongoTemplate;

    private String adminToken;
    private String userToken;
    private String user2Token;
    private String productId;

    @BeforeEach
    void setup() throws Exception {
        adminToken = registerAndGetToken("admin1", "admin@test.com", "pass1234", "ADMIN");
        userToken  = registerAndGetToken("user1",  "user@test.com",  "pass1234", "USER");
        user2Token = registerAndGetToken("user2",  "user2@test.com", "pass1234", "USER");

        // Create a product with 20 units in stock
        ProductRequest.Create req = new ProductRequest.Create();
        req.setName("Widget");
        req.setDescription("A widget");
        req.setPrice(25.00);
        req.setStockQuantity(20);

        MvcResult r = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();

        productId = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    @AfterEach
    void cleanup() { mongoTemplate.getDb().drop(); }

    // ── Place order ───────────────────────────────────────────────────────────

    @Test
    void placeOrder_success_stockDeducted() throws Exception {
        String orderId = placeOrderForUser(userToken, productId, 5);

        // Stock should now be 15
        mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(15));

        // Order should have correct total
        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(125.00))
                .andExpect(jsonPath("$.items[0].quantity").value(5));
    }

    @Test
    void placeOrder_insufficientStock_conflict() throws Exception {
        OrderRequest.PlaceOrder req = buildOrderRequest(productId, 999);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Insufficient Stock"));
    }

    @Test
    void placeOrder_adminRole_forbidden() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildOrderRequest(productId, 1))))
                .andExpect(status().isForbidden());
    }

    @Test
    void placeOrder_invalidProductId_notFound() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildOrderRequest("bad-id", 1))))
                .andExpect(status().isNotFound());
    }

    @Test
    void placeOrder_emptyItems_badRequest() throws Exception {
        OrderRequest.PlaceOrder req = new OrderRequest.PlaceOrder();
        req.setItems(List.of());

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── Confirm ───────────────────────────────────────────────────────────────

    @Test
    void confirmOrder_success() throws Exception {
        String orderId = placeOrderForUser(userToken, productId, 2);

        mockMvc.perform(patch("/api/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void confirmOrder_alreadyConfirmed_badRequest() throws Exception {
        String orderId = placeOrderForUser(userToken, productId, 1);

        mockMvc.perform(patch("/api/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    // ── Cancel ────────────────────────────────────────────────────────────────

    @Test
    void cancelOrder_stockRestored() throws Exception {
        String orderId = placeOrderForUser(userToken, productId, 8);

        // Verify stock is now 12
        mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.stockQuantity").value(12));

        // Cancel the order
        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Stock should be restored to 20
        mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.stockQuantity").value(20));
    }

    @Test
    void cancelOrder_confirmedOrder_badRequest() throws Exception {
        String orderId = placeOrderForUser(userToken, productId, 1);

        mockMvc.perform(patch("/api/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelOrder_alreadyCancelled_badRequest() throws Exception {
        String orderId = placeOrderForUser(userToken, productId, 1);

        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest());
    }

    // ── Order history ─────────────────────────────────────────────────────────

    @Test
    void getOrderHistory_paginated() throws Exception {
        for (int i = 0; i < 4; i++) {
            placeOrderForUser(userToken, productId, 1);
        }

        mockMvc.perform(get("/api/orders?page=0&size=2")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void getOrderHistory_filteredByStatus() throws Exception {
        String orderId = placeOrderForUser(userToken, productId, 1);
        placeOrderForUser(userToken, productId, 1);

        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders?status=CANCELLED")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CANCELLED"));

        mockMvc.perform(get("/api/orders?status=CREATED")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ── Isolation ─────────────────────────────────────────────────────────────

    @Test
    void orderIsolation_userCannotSeeOtherUsersOrder() throws Exception {
        String orderId = placeOrderForUser(userToken, productId, 1);

        mockMvc.perform(get("/api/orders/" + orderId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    void orderIsolation_userCannotCancelOtherUsersOrder() throws Exception {
        String orderId = placeOrderForUser(userToken, productId, 1);

        mockMvc.perform(patch("/api/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String placeOrderForUser(String token, String pId, int qty) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildOrderRequest(pId, qty))))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    private OrderRequest.PlaceOrder buildOrderRequest(String pId, int qty) {
        OrderRequest.OrderItemRequest item = new OrderRequest.OrderItemRequest();
        item.setProductId(pId);
        item.setQuantity(qty);
        OrderRequest.PlaceOrder req = new OrderRequest.PlaceOrder();
        req.setItems(List.of(item));
        return req;
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
