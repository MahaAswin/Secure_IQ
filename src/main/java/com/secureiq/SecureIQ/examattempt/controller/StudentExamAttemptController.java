package com.secureiq.SecureIQ.examattempt.controller;

import com.secureiq.SecureIQ.common.dto.ApiResponse;
import com.secureiq.SecureIQ.examattempt.dto.*;
import com.secureiq.SecureIQ.examattempt.model.AttemptStatus;
import com.secureiq.SecureIQ.examattempt.service.StudentExamAttemptService;
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
@RequestMapping("/api/v1/attempts")
@Tag(name = "Student Exam Attempt Management", description = "Student Exam Attempt Management APIs (start, submit, terminate, listings, dashboards)")
public class StudentExamAttemptController {

    private final StudentExamAttemptService studentExamAttemptService;

    public StudentExamAttemptController(StudentExamAttemptService studentExamAttemptService) {
        this.studentExamAttemptService = studentExamAttemptService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Get list of all exam attempts with pagination, sorting, and filters", description = "Retrieves a paginated list of all attempts.")
    public ApiResponse<Page<StudentExamAttemptResponse>> getAll(
            @RequestParam(required = false) String attemptCode,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long examSessionId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) AttemptStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<StudentExamAttemptResponse> attempts = studentExamAttemptService.getAll(attemptCode, studentId, examSessionId, departmentId, status, pageable);
        return ApiResponse.success(attempts, "Attempts retrieved successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get exam attempt details by ID", description = "Retrieves specific attempt details.")
    public ApiResponse<StudentExamAttemptResponse> getById(@PathVariable Long id) {
        StudentExamAttemptResponse response = studentExamAttemptService.getById(id);
        return ApiResponse.success(response, "Attempt retrieved successfully");
    }

    @PostMapping("/start")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Start a new exam attempt", description = "Begins an attempt for a LIVE exam session.")
    public ApiResponse<StudentExamAttemptResponse> startAttempt(@Valid @RequestBody StartAttemptRequest request) {
        StudentExamAttemptResponse response = studentExamAttemptService.startAttempt(request);
        return ApiResponse.success(response, "Exam attempt started successfully");
    }

    @PatchMapping("/submit")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @Operation(summary = "Submit an exam attempt", description = "Finishes and submits a student's exam attempt.")
    public ApiResponse<StudentExamAttemptResponse> submitAttempt(@Valid @RequestBody SubmitAttemptRequest request) {
        StudentExamAttemptResponse response = studentExamAttemptService.submitAttempt(request);
        return ApiResponse.success(response, "Exam attempt submitted successfully");
    }

    @PatchMapping("/terminate")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY')")
    @Operation(summary = "Terminate an exam attempt", description = "Forcefully terminates a student's active exam attempt.")
    public ApiResponse<StudentExamAttemptResponse> terminateAttempt(@Valid @RequestBody TerminateAttemptRequest request) {
        StudentExamAttemptResponse response = studentExamAttemptService.terminateAttempt(request);
        return ApiResponse.success(response, "Exam attempt terminated successfully");
    }

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get current student's attempts", description = "Retrieves the attempts of the logged-in student.")
    public ApiResponse<Page<StudentExamAttemptResponse>> getMyAttempts(
            @RequestParam(required = false) String attemptCode,
            @RequestParam(required = false) Long examSessionId,
            @RequestParam(required = false) AttemptStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<StudentExamAttemptResponse> attempts = studentExamAttemptService.getMyAttempts(attemptCode, examSessionId, status, pageable);
        return ApiResponse.success(attempts, "Student attempts retrieved successfully");
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Get list of attempts for a session", description = "Retrieves attempts registered under a specific exam session.")
    public ApiResponse<Page<StudentExamAttemptResponse>> getAttemptsBySession(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<StudentExamAttemptResponse> attempts = studentExamAttemptService.getAttemptsBySession(sessionId, pageable);
        return ApiResponse.success(attempts, "Session attempts retrieved successfully");
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get exam attempt dashboard statistics", description = "Retrieves role-scoped statistics for exam attempts.")
    public ApiResponse<AttemptDashboardResponse> getDashboard() {
        AttemptDashboardResponse response = studentExamAttemptService.getDashboard();
        return ApiResponse.success(response, "Attempt dashboard retrieved successfully");
    }
}
