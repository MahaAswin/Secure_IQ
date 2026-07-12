package com.secureiq.SecureIQ.student.service;

import com.secureiq.SecureIQ.student.dto.StudentCreateRequest;
import com.secureiq.SecureIQ.student.dto.StudentPatchRequest;
import com.secureiq.SecureIQ.student.dto.StudentResponse;
import com.secureiq.SecureIQ.student.dto.StudentUpdateRequest;
import com.secureiq.SecureIQ.student.dto.StudentDashboardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentService {
    Page<StudentResponse> getAll(String name, String registerNumber, String rollNumber, Long departmentId, Pageable pageable);
    StudentResponse getById(Long id);
    StudentResponse create(StudentCreateRequest request);
    StudentResponse update(Long id, StudentUpdateRequest request);
    StudentResponse patch(Long id, StudentPatchRequest request);
    void delete(Long id);
    StudentResponse getProfileByCurrentUser();
    StudentDashboardResponse getDashboardByCurrentUser();
}
