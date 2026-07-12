package com.secureiq.SecureIQ.student.controller;

import com.secureiq.SecureIQ.common.dto.ApiResponse;
import com.secureiq.SecureIQ.student.dto.*;
import com.secureiq.SecureIQ.student.service.StudentService;
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
@RequestMapping("/api/v1/students")
@Tag(name = "Student Management", description = "Student Account Management APIs (CRUD, search, pagination, dashboard, profile)")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'HOD')")
    @Operation(summary = "Get list of students with pagination and search", description = "Retrieves a paginated list of students. Access is restricted to Admin, Faculty, and HOD roles. HODs are automatically restricted to their own department's students.")
    public ApiResponse<Page<StudentResponse>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String registerNumber,
            @RequestParam(required = false) String rollNumber,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<StudentResponse> students = studentService.getAll(name, registerNumber, rollNumber, departmentId, pageable);
        return ApiResponse.success(students, "Students retrieved successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'HOD', 'STUDENT')")
    @Operation(summary = "Get student details by ID", description = "Retrieves details of a student. Admins and Faculty can read any profile. HODs can read only within their department. Students can only read their own profile.")
    public ApiResponse<StudentResponse> getById(@PathVariable Long id) {
        StudentResponse response = studentService.getById(id);
        return ApiResponse.success(response, "Student profile retrieved successfully");
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get current student profile", description = "Retrieve profile details of the currently logged-in student user.")
    public ApiResponse<StudentResponse> getMyProfile() {
        StudentResponse response = studentService.getProfileByCurrentUser();
        return ApiResponse.success(response, "Student profile retrieved successfully");
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get current student dashboard details", description = "Fetches stats like upcoming exams count, completed exams count, unread notifications count, and recent activities for the logged-in student.")
    public ApiResponse<StudentDashboardResponse> getMyDashboard() {
        StudentDashboardResponse response = studentService.getDashboardByCurrentUser();
        return ApiResponse.success(response, "Student dashboard retrieved successfully");
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new student profile", description = "Create student record and link it to an existing user. Access limited to ADMIN only.")
    public ApiResponse<StudentResponse> create(@Valid @RequestBody StudentCreateRequest request) {
        StudentResponse response = studentService.create(request);
        return ApiResponse.success(response, "Student created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    @Operation(summary = "Update an existing student profile", description = "Update student details by ID. Admin can update anyone. Student can update their own profile only.")
    public ApiResponse<StudentResponse> update(@PathVariable Long id, @Valid @RequestBody StudentUpdateRequest request) {
        StudentResponse response = studentService.update(id, request);
        return ApiResponse.success(response, "Student updated successfully");
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STUDENT')")
    @Operation(summary = "Partially update an existing student profile", description = "Partially update student details by ID. Admin can patch anyone. Student can patch their own profile only.")
    public ApiResponse<StudentResponse> patch(@PathVariable Long id, @Valid @RequestBody StudentPatchRequest request) {
        StudentResponse response = studentService.patch(id, request);
        return ApiResponse.success(response, "Student updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft delete a student profile", description = "Performs a soft delete on a student record. Access limited to ADMIN only.")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return ApiResponse.success("Student deleted successfully");
    }
}
