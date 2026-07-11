package com.secureiq.SecureIQ.auth.service;

import com.secureiq.SecureIQ.auth.dto.AuthResponse;
import com.secureiq.SecureIQ.auth.dto.LoginRequest;
import com.secureiq.SecureIQ.auth.dto.RegisterRequest;
import com.secureiq.SecureIQ.user.dto.UserResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    UserResponse getMe();
}
