package com.secureiq.SecureIQ.faculty.controller;

import com.secureiq.SecureIQ.common.dto.ApiResponse;
import com.secureiq.SecureIQ.faculty.dto.*;
import com.secureiq.SecureIQ.faculty.service.FacultyService;
import com.secureiq.SecureIQ.student.dto.RecentActivityResponse;
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
@RequestMapping("/api/v1/faculties")
@Tag(name = "Faculty Management", description = "Faculty Account Management APIs (CRUD, search, pagination, dashboard, profile)")
public class FacultyController {

    private final FacultyService facultyService;

    public FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get list of faculties with pagination and search", description = "Retrieves a paginated list of faculties. HODs, Faculty, and Students are restricted to their own department's faculty list.")
    public ApiResponse<Page<FacultyResponse>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String specialization,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<FacultyResponse> faculties = facultyService.getAll(name, employeeId, departmentId, specialization, pageable);
        return ApiResponse.success(faculties, "Faculties retrieved successfully");
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY', 'STUDENT')")
    @Operation(summary = "Get faculty details by ID", description = "Retrieves details of a faculty member. Admins have unrestricted access. Other roles are restricted to their department's faculties, and faculty members can always view their own profile.")
    public ApiResponse<FacultyResponse> getById(@PathVariable Long id) {
        FacultyResponse response = facultyService.getById(id);
        return ApiResponse.success(response, "Faculty profile retrieved successfully");
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('FACULTY')")
    @Operation(summary = "Get current faculty profile", description = "Retrieve profile details of the currently logged-in faculty user.")
    public ApiResponse<FacultyResponse> getMyProfile() {
        FacultyResponse response = facultyService.getProfileByCurrentUser();
        return ApiResponse.success(response, "Faculty profile retrieved successfully");
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('FACULTY')")
    @Operation(summary = "Get current faculty dashboard details", description = "Fetches stats like assigned subjects count, assigned students count, upcoming exams count, and recent activities for the logged-in faculty.")
    public ApiResponse<FacultyDashboardResponse> getMyDashboard() {
        FacultyDashboardResponse response = facultyService.getCombinedDashboard();
        return ApiResponse.success(response, "Faculty dashboard retrieved successfully");
    }

    @GetMapping("/dashboard/subjects")
    @PreAuthorize("hasRole('FACULTY')")
    @Operation(summary = "Get assigned subjects", description = "Fetches all subjects assigned to the logged-in faculty member.")
    public ApiResponse<List<FacultyResponse.SubjectDto>> getAssignedSubjects() {
        List<FacultyResponse.SubjectDto> response = facultyService.getAssignedSubjects();
        return ApiResponse.success(response, "Assigned subjects retrieved successfully");
    }

    @GetMapping("/dashboard/students")
    @PreAuthorize("hasRole('FACULTY')")
    @Operation(summary = "Get assigned students", description = "Fetches all students assigned to the logged-in faculty member (students in the department and semester of subjects taught by the faculty, or fallback to their department's students).")
    public ApiResponse<List<StudentResponse>> getAssignedStudents() {
        List<StudentResponse> response = facultyService.getAssignedStudents();
        return ApiResponse.success(response, "Assigned students retrieved successfully");
    }

    @GetMapping("/dashboard/exams")
    @PreAuthorize("hasRole('FACULTY')")
    @Operation(summary = "Get upcoming exams", description = "Fetches upcoming exams for the departments of subjects taught by the logged-in faculty member.")
    public ApiResponse<List<FacultyDashboardResponse.ExamDto>> getUpcomingExams() {
        List<FacultyDashboardResponse.ExamDto> response = facultyService.getUpcomingExams();
        return ApiResponse.success(response, "Upcoming exams retrieved successfully");
    }

    @GetMapping("/dashboard/activities")
    @PreAuthorize("hasRole('FACULTY')")
    @Operation(summary = "Get recent activities", description = "Fetches recent student activities for students assigned to the logged-in faculty member.")
    public ApiResponse<List<RecentActivityResponse>> getRecentActivities() {
        List<RecentActivityResponse> response = facultyService.getRecentActivities();
        return ApiResponse.success(response, "Recent activities retrieved successfully");
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Create a new faculty profile", description = "Create faculty record and link it to an existing user. Admin can create for any department. HOD can only create within their department.")
    public ApiResponse<FacultyResponse> create(@Valid @RequestBody FacultyCreateRequest request) {
        FacultyResponse response = facultyService.create(request);
        return ApiResponse.success(response, "Faculty created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Update an existing faculty profile", description = "Update faculty details by ID. Admin and HOD (in department) can update anyone. Faculty can update their own profile only.")
    public ApiResponse<FacultyResponse> update(@PathVariable Long id, @Valid @RequestBody FacultyUpdateRequest request) {
        FacultyResponse response = facultyService.update(id, request);
        return ApiResponse.success(response, "Faculty updated successfully");
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD', 'FACULTY')")
    @Operation(summary = "Partially update an existing faculty profile", description = "Partially update faculty details by ID. Admin and HOD (in department) can patch anyone. Faculty can patch their own profile only.")
    public ApiResponse<FacultyResponse> patch(@PathVariable Long id, @Valid @RequestBody FacultyPatchRequest request) {
        FacultyResponse response = facultyService.patch(id, request);
        return ApiResponse.success(response, "Faculty updated successfully");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOD')")
    @Operation(summary = "Soft delete a faculty profile", description = "Performs a soft delete on a faculty record. Access restricted to ADMIN and HOD (restricted to their department).")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        facultyService.delete(id);
        return ApiResponse.success("Faculty deleted successfully");
    }
}
