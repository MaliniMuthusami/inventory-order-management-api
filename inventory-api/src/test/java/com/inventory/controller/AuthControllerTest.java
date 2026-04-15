package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.dto.request.AuthRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MongoTemplate mongoTemplate;

    @AfterEach
    void cleanup() { mongoTemplate.getDb().drop(); }

    @Test
    void register_asUser_success() throws Exception {
        AuthRequest.Register req = buildRegister("alice", "alice@test.com", "pass1234");
        mockMvc.perform(post("/api/auth/register?role=USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.username").value("alice"));
    }

    @Test
    void register_asAdmin_success() throws Exception {
        AuthRequest.Register req = buildRegister("admin1", "admin@test.com", "pass1234");
        mockMvc.perform(post("/api/auth/register?role=ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    void register_duplicateUsername_conflict() throws Exception {
        AuthRequest.Register req = buildRegister("dup", "dup@test.com", "pass1234");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        AuthRequest.Register dup = buildRegister("dup", "other@test.com", "pass1234");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_duplicateEmail_conflict() throws Exception {
        AuthRequest.Register req = buildRegister("user1", "shared@test.com", "pass1234");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        AuthRequest.Register dup = buildRegister("user2", "shared@test.com", "pass1234");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidEmail_badRequest() throws Exception {
        AuthRequest.Register req = buildRegister("bob", "not-an-email", "pass1234");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void register_shortPassword_badRequest() throws Exception {
        AuthRequest.Register req = buildRegister("carol", "carol@test.com", "abc");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void login_success() throws Exception {
        AuthRequest.Register reg = buildRegister("dave", "dave@test.com", "pass1234");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        AuthRequest.Login login = new AuthRequest.Login();
        login.setUsername("dave");
        login.setPassword("pass1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("dave"));
    }

    @Test
    void login_wrongPassword_unauthorized() throws Exception {
        AuthRequest.Register reg = buildRegister("eve", "eve@test.com", "correct");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        AuthRequest.Login login = new AuthRequest.Login();
        login.setUsername("eve");
        login.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownUser_unauthorized() throws Exception {
        AuthRequest.Login login = new AuthRequest.Login();
        login.setUsername("ghost");
        login.setPassword("pass1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private AuthRequest.Register buildRegister(String username, String email, String password) {
        AuthRequest.Register r = new AuthRequest.Register();
        r.setUsername(username);
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }
}
