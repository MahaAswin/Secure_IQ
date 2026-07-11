package com.secureiq.SecureIQ.user.mapper;

import com.secureiq.SecureIQ.user.dto.UserCreateRequest;
import com.secureiq.SecureIQ.user.dto.UserResponse;
import com.secureiq.SecureIQ.user.dto.UserUpdateRequest;
import com.secureiq.SecureIQ.user.model.Status;
import com.secureiq.SecureIQ.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.fromEntity(user);
    }

    public User toEntity(UserCreateRequest request) {
        if (request == null) {
            return null;
        }
        return User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(request.getPassword())
                .role(request.getRole())
                .status(request.getStatus() != null ? request.getStatus() : Status.ACTIVE)
                .profileImage(request.getProfileImage())
                .deleted(false)
                .build();
    }

    public void updateEntity(UserUpdateRequest request, User user) {
        if (request == null || user == null) {
            return;
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());
        user.setProfileImage(request.getProfileImage());
    }
}
