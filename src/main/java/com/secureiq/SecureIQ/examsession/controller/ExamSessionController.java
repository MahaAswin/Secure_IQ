package com.secureiq.SecureIQ.examsession.controller;

import com.secureiq.SecureIQ.common.dto.ApiResponse;
import com.secureiq.SecureIQ.examsession.dto.*;
import com.secureiq.SecureIQ.examsession.model.SessionStatus;
import com.secureiq.SecureIQ.examsession.service.ExamSessionService;
import com.secureiq.SecureIQ.student.dto.StudentResponse;
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
@RequestMapping("/api/v1/exam-sessions")
@Tag(name = "Exam Session Management", description = "Exam Session Management APIs (CRUD, student assignments, lists, dashboard)")
public class ExamSessionController {

    private final ExamSessionService examSessionService;

    public ExamSessionController(ExamSessionService examSessionService) {
        this.examSessionService = examSessionService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get list of exam sessions with pagination, sorting, and filters", description = "Retrieves a paginated list of exam sessions based on search criteria and permissions.")
    public ApiResponse<Page<ExamSessionResponse>> getAll(
            @RequestParam(required = false) String sessionCode,
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) SessionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ExamSessionResponse> sessions = examSessionService.getAll(sessionCode, examId, facultyId, departmentId, status, pageable);
        return ApiResponse.success(sessions, "Exam sessions retrieved successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get exam session details by ID", description = "Retrieves a specific exam session's details.")
    public ApiResponse<ExamSessionResponse> getById(@PathVariable Long id) {
        ExamSessionResponse response = examSessionService.getById(id);
        return ApiResponse.success(response, "Exam session retrieved successfully");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Create a new exam session", description = "Create a new exam session.")
    public ApiResponse<ExamSessionResponse> create(@Valid @RequestBody ExamSessionCreateRequest request) {
        ExamSessionResponse response = examSessionService.create(request);
        return ApiResponse.success(response, "Exam session created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Update an existing exam session", description = "Fully update exam session details by ID.")
    public ApiResponse<ExamSessionResponse> update(@PathVariable Long id, @Valid @RequestBody ExamSessionUpdateRequest request) {
        ExamSessionResponse response = examSessionService.update(id, request);
        return ApiResponse.success(response, "Exam session updated successfully");
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Partially update an existing exam session", description = "Partially update exam session details by ID.")
    public ApiResponse<ExamSessionResponse> patch(@PathVariable Long id, @Valid @RequestBody ExamSessionPatchRequest request) {
        ExamSessionResponse response = examSessionService.patch(id, request);
        return ApiResponse.success(response, "Exam session updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Soft delete an exam session", description = "Performs a soft delete on an exam session record.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        examSessionService.delete(id);
        return ApiResponse.success("Exam session deleted successfully");
    }

    @PostMapping("/{id}/assign-students")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Assign students to an exam session", description = "Links multiple students to the specified exam session.")
    public ApiResponse<ExamSessionResponse> assignStudents(
            @PathVariable Long id,
            @Valid @RequestBody StudentAssignmentRequest request) {
        ExamSessionResponse response = examSessionService.assignStudents(id, request);
        return ApiResponse.success(response, "Students assigned successfully");
    }

    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get list of assigned students in a session", description = "Retrieves the list of students assigned to the specified exam session.")
    public ApiResponse<List<StudentResponse>> getAssignedStudents(@PathVariable Long id) {
        List<StudentResponse> response = examSessionService.getAssignedStudents(id);
        return ApiResponse.success(response, "Assigned students retrieved successfully");
    }

    @GetMapping("/live")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get list of live exam sessions", description = "Retrieves the list of active/live sessions that are currently visible to the caller.")
    public ApiResponse<List<ExamSessionResponse>> getLiveSessions() {
        List<ExamSessionResponse> response = examSessionService.getLiveSessions();
        return ApiResponse.success(response, "Live sessions retrieved successfully");
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get list of upcoming exam sessions", description = "Retrieves the list of scheduled/upcoming sessions in the future.")
    public ApiResponse<List<ExamSessionResponse>> getUpcomingSessions() {
        List<ExamSessionResponse> response = examSessionService.getUpcomingSessions();
        return ApiResponse.success(response, "Upcoming sessions retrieved successfully");
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get exam session dashboard statistics", description = "Retrieves role-scoped dashboard metrics for students, faculty, or administration.")
    public ApiResponse<ExamSessionDashboardResponse> getDashboard() {
        ExamSessionDashboardResponse response = examSessionService.getDashboard();
        return ApiResponse.success(response, "Exam session dashboard retrieved successfully");
    }
}
