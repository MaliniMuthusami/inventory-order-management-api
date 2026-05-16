package com.inventory.controller;

import com.inventory.dto.request.AuthRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register and login")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user. Use role=ADMIN or role=USER")
    public ResponseEntity<ApiResponse.Auth> register(
            @Valid @RequestBody AuthRequest.Register request,
            @RequestParam(defaultValue = "USER") String role) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, role));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive a JWT token")
    public ResponseEntity<ApiResponse.Auth> login(@Valid @RequestBody AuthRequest.Login request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
