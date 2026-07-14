package com.secureiq.SecureIQ.exam.controller;

import com.secureiq.SecureIQ.common.dto.ApiResponse;
import com.secureiq.SecureIQ.exam.dto.*;
import com.secureiq.SecureIQ.exam.model.ExamStatus;
import com.secureiq.SecureIQ.exam.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exams")
@Tag(name = "Exam Management", description = "Exam Management APIs (CRUD, filtering, dashboard, scheduling)")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get list of exams with pagination and search", description = "Retrieves a paginated list of exams. Filters are applied dynamically. HODs, Faculty and Students are restricted to viewing relevant exams.")
    public ApiResponse<Page<ExamResponse>> getAll(
            @RequestParam(required = false) String examTitle,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) ExamStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ExamResponse> exams = examService.getAll(examTitle, subjectId, facultyId, departmentId, status, pageable);
        return ApiResponse.success(exams, "Exams retrieved successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get exam details by ID", description = "Retrieves details of an exam. Appropriate authorization checks are applied based on the caller's role.")
    public ApiResponse<ExamResponse> getById(@PathVariable Long id) {
        ExamResponse response = examService.getById(id);
        return ApiResponse.success(response, "Exam details retrieved successfully");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Create a new exam", description = "Create an exam. HODs can only create for their own department. Faculty can only create for themselves.")
    public ApiResponse<ExamResponse> create(@Valid @RequestBody ExamCreateRequest request) {
        ExamResponse response = examService.create(request);
        return ApiResponse.success(response, "Exam created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Update an existing exam", description = "Fully updates an exam. HODs can only update exams within their department. Faculty can only update their own exams.")
    public ApiResponse<ExamResponse> update(@PathVariable Long id, @Valid @RequestBody ExamUpdateRequest request) {
        ExamResponse response = examService.update(id, request);
        return ApiResponse.success(response, "Exam updated successfully");
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Partially update an existing exam", description = "Partially updates an exam. HODs can only update exams within their department. Faculty can only update their own exams.")
    public ApiResponse<ExamResponse> patch(@PathVariable Long id, @Valid @RequestBody ExamPatchRequest request) {
        ExamResponse response = examService.patch(id, request);
        return ApiResponse.success(response, "Exam updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Delete an exam", description = "Performs soft delete on an exam. HODs can only delete within their department. Faculty can only delete their own exams.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        examService.delete(id);
        return ApiResponse.success("Exam deleted successfully");
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get list of upcoming exams", description = "Retrieves upcoming exams for the authenticated user based on their role and department/semester restrictions.")
    public ApiResponse<List<ExamResponse>> getUpcoming() {
        List<ExamResponse> response = examService.getUpcomingExams();
        return ApiResponse.success(response, "Upcoming exams retrieved successfully");
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get today's exams", description = "Retrieves exams scheduled for today for the authenticated user based on their role and department/semester restrictions.")
    public ApiResponse<List<ExamResponse>> getToday() {
        List<ExamResponse> response = examService.getTodaysExams();
        return ApiResponse.success(response, "Today's exams retrieved successfully");
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get active exams", description = "Retrieves active exams for the authenticated user based on their role and department/semester restrictions.")
    public ApiResponse<List<ExamResponse>> getActive() {
        List<ExamResponse> response = examService.getActiveExams();
        return ApiResponse.success(response, "Active exams retrieved successfully");
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get exam dashboard details", description = "Returns dashboard metrics and exam lists appropriate for the caller's role.")
    public ApiResponse<ExamDashboardResponse> getDashboard() {
        ExamDashboardResponse response = examService.getDashboard();
        return ApiResponse.success(response, "Exam dashboard retrieved successfully");
    }
}
