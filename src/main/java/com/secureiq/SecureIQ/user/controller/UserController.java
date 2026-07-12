package com.secureiq.SecureIQ.user.controller;

import com.secureiq.SecureIQ.common.dto.ApiResponse;
import com.secureiq.SecureIQ.user.dto.UserCreateRequest;
import com.secureiq.SecureIQ.user.dto.UserPatchRequest;
import com.secureiq.SecureIQ.user.dto.UserResponse;
import com.secureiq.SecureIQ.user.dto.UserStatusUpdateRequest;
import com.secureiq.SecureIQ.user.dto.UserUpdateRequest;
import com.secureiq.SecureIQ.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User Account Management APIs (CRUD, search, pagination, status patching, authorization checks)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Get list of users with pagination and search", description = "Retrieves a paginated list of users. Admin and HOD roles have access. Supports searching by name and email.")
    public ApiResponse<Page<UserResponse>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<UserResponse> users = userService.getAll(name, email, pageable);
        return ApiResponse.success(users, "Users retrieved successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD') or authentication.principal.id == #id")
    @Operation(summary = "Get user details by ID", description = "Retrieve a user's details. Admin and HOD can access any profile. Faculty and Students can only access their own profile.")
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        UserResponse response = userService.getById(id);
        return ApiResponse.success(response, "User profile retrieved successfully");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new user", description = "Create a new user account. Access limited to ADMIN only.")
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.create(request);
        return ApiResponse.success(response, "User created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #id")
    @Operation(summary = "Update an existing user", description = "Update user details by ID. Admin can update anyone, others can only update their own profile.")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.update(id, request);
        return ApiResponse.success(response, "User updated successfully");
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #id")
    @Operation(summary = "Partially update an existing user", description = "Partially update user details by ID. Admin can update anyone, others can only update their own profile.")
    public ApiResponse<UserResponse> patch(@PathVariable Long id, @Valid @RequestBody UserPatchRequest request) {
        UserResponse response = userService.patch(id, request);
        return ApiResponse.success(response, "User updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete a user", description = "Performs a soft delete by setting deleted=true and appending a suffix to their email. Access limited to ADMIN only.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success("User deleted successfully");
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user status", description = "Update the active status (ACTIVE, INACTIVE, BLOCKED) of a user account. Access limited to ADMIN only.")
    public ApiResponse<UserResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusUpdateRequest request) {
        UserResponse response = userService.updateStatus(id, request.getStatus());
        return ApiResponse.success(response, "User status updated successfully");
    }
}
