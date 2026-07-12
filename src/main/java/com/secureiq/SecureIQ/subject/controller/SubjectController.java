package com.secureiq.SecureIQ.subject.controller;

import com.secureiq.SecureIQ.common.dto.ApiResponse;
import com.secureiq.SecureIQ.subject.dto.*;
import com.secureiq.SecureIQ.subject.service.SubjectService;
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
@RequestMapping("/api/v1/subjects")
@Tag(name = "Subject Management", description = "Subject Management APIs (CRUD, search, pagination, dashboard)")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'HOD', 'STUDENT')")
    @Operation(summary = "Get list of subjects with pagination and search", description = "Retrieves a paginated list of subjects. Admins see all. Faculty see assigned. HODs see own department. Students see own department/semester.")
    public ApiResponse<Page<SubjectResponse>> getAll(
            @RequestParam(required = false) String subjectName,
            @RequestParam(required = false) String subjectCode,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<SubjectResponse> subjects = subjectService.getAll(subjectName, subjectCode, semester, departmentId, pageable);
        return ApiResponse.success(subjects, "Subjects retrieved successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'HOD', 'STUDENT')")
    @Operation(summary = "Get subject details by ID", description = "Retrieves a specific subject's details. Same visibility restrictions as listings apply.")
    public ApiResponse<SubjectResponse> getById(@PathVariable Long id) {
        SubjectResponse response = subjectService.getById(id);
        return ApiResponse.success(response, "Subject retrieved successfully");
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'HOD', 'STUDENT')")
    @Operation(summary = "Get subject dashboard statistics", description = "Retrieves dynamic dashboard metrics filtered by role permissions.")
    public ApiResponse<SubjectDashboardResponse> getDashboard() {
        SubjectDashboardResponse response = subjectService.getDashboard();
        return ApiResponse.success(response, "Subject dashboard retrieved successfully");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Create a new subject", description = "Create a new subject. Admin has full rights. HOD can only create within their department.")
    public ApiResponse<SubjectResponse> create(@Valid @RequestBody SubjectCreateRequest request) {
        SubjectResponse response = subjectService.create(request);
        return ApiResponse.success(response, "Subject created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Update an existing subject", description = "Fully update subject details by ID. Admin has full rights. HOD can only update within their department.")
    public ApiResponse<SubjectResponse> update(@PathVariable Long id, @Valid @RequestBody SubjectUpdateRequest request) {
        SubjectResponse response = subjectService.update(id, request);
        return ApiResponse.success(response, "Subject updated successfully");
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Partially update an existing subject", description = "Partially update subject details by ID. Admin has full rights. HOD can only update within their department.")
    public ApiResponse<SubjectResponse> patch(@PathVariable Long id, @Valid @RequestBody SubjectPatchRequest request) {
        SubjectResponse response = subjectService.patch(id, request);
        return ApiResponse.success(response, "Subject updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Soft delete a subject", description = "Performs a soft delete on a subject record. Admin has full rights. HOD can only delete within their department.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subjectService.delete(id);
        return ApiResponse.success("Subject deleted successfully");
    }
}
