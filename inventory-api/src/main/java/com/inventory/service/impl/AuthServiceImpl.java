package com.inventory.service.impl;

import com.inventory.model.Role;
import com.inventory.model.User;
import com.inventory.dto.request.AuthRequest;
import com.inventory.dto.response.ApiResponse;
import com.inventory.exception.BadRequestException;
import com.inventory.exception.DuplicateResourceException;
import com.inventory.repository.UserRepository;
import com.inventory.security.JwtUtils;
import com.inventory.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public ApiResponse.Auth register(AuthRequest.Register request, String roleName) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email '" + request.getEmail() + "' is already registered");
        }

        Role role;
        try {
            role = Role.valueOf(roleName.toUpperCase().startsWith("ROLE_") ? roleName.toUpperCase() : "ROLE_" + roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid role. Use ADMIN or USER");
        }

        User user = new User(request.getUsername(), request.getEmail(),
                passwordEncoder.encode(request.getPassword()), role);
        userRepository.save(user);
        log.info("User '{}' registered with role {}", user.getUsername(), role);

        String token = jwtUtils.generateToken(user);
        return new ApiResponse.Auth(token, "Bearer", user.getUsername(), user.getEmail(), role);
    }

    @Override
    public ApiResponse.Auth login(AuthRequest.Login request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(auth);
        User user = (User) auth.getPrincipal();
        log.info("User '{}' logged in", user.getUsername());
        String token = jwtUtils.generateToken(user);
        return new ApiResponse.Auth(token, "Bearer", user.getUsername(), user.getEmail(), user.getRole());
    }
}
