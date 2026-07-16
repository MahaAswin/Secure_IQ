package com.secureiq.SecureIQ.violation.controller;

import com.secureiq.SecureIQ.common.dto.ApiResponse;
import com.secureiq.SecureIQ.violation.dto.*;
import com.secureiq.SecureIQ.violation.model.Severity;
import com.secureiq.SecureIQ.violation.model.Source;
import com.secureiq.SecureIQ.violation.model.ViolationType;
import com.secureiq.SecureIQ.violation.service.ViolationService;
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
@RequestMapping("/api/v1/violations")
@Tag(name = "Violation Management", description = "Violation Management APIs (record, update/resolve, search, dashboard)")
public class ViolationController {

    private final ViolationService violationService;

    public ViolationController(ViolationService violationService) {
        this.violationService = violationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Get list of all violations with pagination, sorting, and filters", description = "Retrieves a paginated list of violations based on criteria.")
    public ApiResponse<Page<ViolationResponse>> getAll(
            @RequestParam(required = false) String violationCode,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long sessionId,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) ViolationType type,
            @RequestParam(required = false) Source source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ViolationResponse> violations = violationService.getAll(violationCode, studentId, sessionId, severity, type, source, pageable);
        return ApiResponse.success(violations, "Violations retrieved successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get details of a violation by ID", description = "Retrieves a specific violation record.")
    public ApiResponse<ViolationResponse> getById(@PathVariable Long id) {
        ViolationResponse response = violationService.getById(id);
        return ApiResponse.success(response, "Violation retrieved successfully");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Record a new violation", description = "Logs a proctoring violation against a student's attempt.")
    public ApiResponse<ViolationResponse> recordViolation(@Valid @RequestBody ViolationCreateRequest request) {
        ViolationResponse response = violationService.recordViolation(request);
        return ApiResponse.success(response, "Violation recorded successfully");
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    @Operation(summary = "Update or resolve an existing violation by ID", description = "Updates details, actions taken, or resolution status of a violation.")
    public ApiResponse<ViolationResponse> updateViolation(
            @PathVariable Long id,
            @Valid @RequestBody ViolationUpdateRequest request) {
        ViolationResponse response = violationService.updateViolation(id, request);
        return ApiResponse.success(response, "Violation updated successfully");
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get list of violations for a specific student", description = "Retrieves violations filtered by student ID.")
    public ApiResponse<Page<ViolationResponse>> getViolationsByStudent(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ViolationResponse> response = violationService.getViolationsByStudent(studentId, pageable);
        return ApiResponse.success(response, "Student violations retrieved successfully");
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Get list of violations for a specific session", description = "Retrieves violations filtered by exam session ID.")
    public ApiResponse<Page<ViolationResponse>> getViolationsBySession(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ViolationResponse> response = violationService.getViolationsBySession(sessionId, pageable);
        return ApiResponse.success(response, "Session violations retrieved successfully");
    }

    @GetMapping("/live")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Get live violations listing for active sessions", description = "Retrieves live violations feed from currently running sessions.")
    public ApiResponse<Page<ViolationResponse>> getLiveViolations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ViolationResponse> response = violationService.getLiveViolations(pageable);
        return ApiResponse.success(response, "Live violations retrieved successfully");
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get violation dashboard metrics", description = "Retrieves role-scoped proctoring reports and statistics.")
    public ApiResponse<ViolationDashboardResponse> getDashboard() {
        ViolationDashboardResponse response = violationService.getDashboard();
        return ApiResponse.success(response, "Violation dashboard retrieved successfully");
    }
}
