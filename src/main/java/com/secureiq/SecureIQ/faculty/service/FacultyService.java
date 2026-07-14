package com.secureiq.SecureIQ.faculty.service;

import com.secureiq.SecureIQ.faculty.dto.*;
import com.secureiq.SecureIQ.student.dto.StudentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FacultyService {
    Page<FacultyResponse> getAll(String name, String employeeId, Long departmentId, String specialization, Pageable pageable);
    FacultyResponse getById(Long id);
    FacultyResponse getProfileByCurrentUser();
    FacultyResponse create(FacultyCreateRequest request);
    FacultyResponse update(Long id, FacultyUpdateRequest request);
    FacultyResponse patch(Long id, FacultyPatchRequest request);
    void delete(Long id);
    List<FacultyResponse.SubjectDto> getAssignedSubjects();
    List<StudentResponse> getAssignedStudents();
    List<FacultyDashboardResponse.ExamDto> getUpcomingExams();
    List<com.secureiq.SecureIQ.student.dto.RecentActivityResponse> getRecentActivities();
    FacultyDashboardResponse getCombinedDashboard();
}
