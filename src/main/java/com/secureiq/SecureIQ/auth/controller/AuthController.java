package com.secureiq.SecureIQ.auth.controller;

import com.secureiq.SecureIQ.auth.dto.AuthResponse;
import com.secureiq.SecureIQ.auth.dto.LoginRequest;
import com.secureiq.SecureIQ.auth.dto.RegisterRequest;
import com.secureiq.SecureIQ.common.dto.ApiResponse;
import com.secureiq.SecureIQ.auth.service.AuthService;
import com.secureiq.SecureIQ.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and User Registration Management APIs")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Allows students, faculty, HODs, or admins to register.")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ApiResponse.success(response, "User registered successfully");
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Generates a JWT bearer token for valid user credentials.")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ApiResponse.success(response, "Logged in successfully");
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Fetches the details of the currently authenticated user session.")
    public ApiResponse<UserResponse> getMe() {
        UserResponse response = authService.getMe();
        return ApiResponse.success(response, "User profile retrieved successfully");
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Clears the security context and invalidates the session.")
    public ApiResponse<Void> logout() {
        SecurityContextHolder.clearContext();
        return ApiResponse.success("Logged out successfully");
    }
}
