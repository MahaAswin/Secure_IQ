package com.secureiq.SecureIQ.department.controller;

import com.secureiq.SecureIQ.common.dto.ApiResponse;
import com.secureiq.SecureIQ.department.dto.*;
import com.secureiq.SecureIQ.department.service.DepartmentService;
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
@RequestMapping("/api/v1/departments")
@Tag(name = "Department Management", description = "Department Management APIs (CRUD, search, pagination, dashboard)")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'HOD', 'STUDENT')")
    @Operation(summary = "Get list of departments with pagination and search", description = "Retrieves a paginated list of departments. Admins and Faculty see all departments. HODs see only their own. Students see only their own.")
    public ApiResponse<Page<DepartmentResponse>> getAll(
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) String departmentCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<DepartmentResponse> departments = departmentService.getAll(departmentName, departmentCode, pageable);
        return ApiResponse.success(departments, "Departments retrieved successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'HOD', 'STUDENT')")
    @Operation(summary = "Get department details by ID", description = "Retrieves a specific department's details. Admins and Faculty can read any department. HODs and Students can read only their own department.")
    public ApiResponse<DepartmentResponse> getById(@PathVariable Long id) {
        DepartmentResponse response = departmentService.getById(id);
        return ApiResponse.success(response, "Department retrieved successfully");
    }

    @GetMapping("/{id}/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'HOD', 'STUDENT')")
    @Operation(summary = "Get department dashboard details by ID", description = "Retrieves dashboard stats for a department (total students, total faculty, HOD details). Same visibility restrictions as details fetch apply.")
    public ApiResponse<DepartmentDashboardResponse> getDashboard(@PathVariable Long id) {
        DepartmentDashboardResponse response = departmentService.getDashboard(id);
        return ApiResponse.success(response, "Department dashboard retrieved successfully");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new department", description = "Create a new department. Access limited to ADMIN only.")
    public ApiResponse<DepartmentResponse> create(@Valid @RequestBody DepartmentCreateRequest request) {
        DepartmentResponse response = departmentService.create(request);
        return ApiResponse.success(response, "Department created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing department", description = "Fully update department details by ID. Access limited to ADMIN only.")
    public ApiResponse<DepartmentResponse> update(@PathVariable Long id, @Valid @RequestBody DepartmentUpdateRequest request) {
        DepartmentResponse response = departmentService.update(id, request);
        return ApiResponse.success(response, "Department updated successfully");
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Partially update an existing department", description = "Partially update department details by ID. Access limited to ADMIN only.")
    public ApiResponse<DepartmentResponse> patch(@PathVariable Long id, @Valid @RequestBody DepartmentPatchRequest request) {
        DepartmentResponse response = departmentService.patch(id, request);
        return ApiResponse.success(response, "Department updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete a department", description = "Performs a soft delete on a department record. Access limited to ADMIN only.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.success("Department deleted successfully");
    }
}
