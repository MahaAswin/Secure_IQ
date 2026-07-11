package com.secureiq.SecureIQ.user.service;

import com.secureiq.SecureIQ.user.dto.UserCreateRequest;
import com.secureiq.SecureIQ.user.dto.UserResponse;
import com.secureiq.SecureIQ.user.dto.UserUpdateRequest;
import com.secureiq.SecureIQ.user.model.Status;
import com.secureiq.SecureIQ.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    Page<UserResponse> getAll(String name, String email, Pageable pageable);
    UserResponse getById(Long id);
    UserResponse getByEmail(String email);
    UserResponse create(UserCreateRequest request);
    UserResponse update(Long id, UserUpdateRequest request);
    void delete(Long id);
    UserResponse updateStatus(Long id, Status status);
    User getEntityById(Long id);
    User getEntityByEmail(String email);
}
