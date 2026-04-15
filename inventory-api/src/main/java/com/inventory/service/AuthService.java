package com.inventory.service;

import com.inventory.dto.request.AuthRequest;
import com.inventory.dto.response.ApiResponse;

public interface AuthService {
    ApiResponse.Auth register(AuthRequest.Register request, String roleName);
    ApiResponse.Auth login(AuthRequest.Login request);
}
