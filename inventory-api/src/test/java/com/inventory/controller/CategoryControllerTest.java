package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.AuthRequest;
import com.inventory.dto.request.CategoryRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MongoTemplate mongoTemplate;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setup() throws Exception {
        adminToken = registerAndGetToken("admin1", "admin@test.com", "pass1234", "ADMIN");
        userToken  = registerAndGetToken("user1",  "user@test.com",  "pass1234", "USER");
    }

    @AfterEach
    void cleanup() { mongoTemplate.getDb().drop(); }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Test
    void createCategory_admin_success() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCategory("Electronics", "Electronic goods"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void createCategory_user_forbidden() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCategory("Electronics", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCategory_noToken_forbidden() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCategory("Electronics", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCategory_duplicate_conflict() throws Exception {
        CategoryRequest req = buildCategory("Books", "Book category");

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void createCategory_missingName_badRequest() throws Exception {
        CategoryRequest req = new CategoryRequest();
        mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void getAllCategories_admin_success() throws Exception {
        for (String name : new String[]{"Sports", "Food", "Clothing"}) {
            mockMvc.perform(post("/api/categories")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildCategory(name, null))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/categories").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void getCategoryById_success() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCategory("Toys", "Kids toys"))))
                .andExpect(status().isCreated()).andReturn();

        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/categories/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Toys"))
                .andExpect(jsonPath("$.description").value("Kids toys"));
    }

    @Test
    void updateCategory_success() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCategory("OldName", null))))
                .andReturn();

        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/categories/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCategory("NewName", "Updated desc"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("NewName"))
                .andExpect(jsonPath("$.description").value("Updated desc"));
    }

    @Test
    void deleteCategory_success() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCategory("ToDelete", null))))
                .andReturn();

        String id = objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(delete("/api/categories/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/categories/" + id).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private CategoryRequest buildCategory(String name, String desc) {
        CategoryRequest r = new CategoryRequest();
        r.setName(name);
        r.setDescription(desc);
        return r;
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
