package com.secureiq.SecureIQ.exam.service;

import com.secureiq.SecureIQ.exam.dto.*;
import com.secureiq.SecureIQ.exam.model.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ExamService {
    Page<ExamResponse> getAll(String examTitle, Long subjectId, Long facultyId, Long departmentId, ExamStatus status, Pageable pageable);
    ExamResponse getById(Long id);
    ExamResponse create(ExamCreateRequest request);
    ExamResponse update(Long id, ExamUpdateRequest request);
    ExamResponse patch(Long id, ExamPatchRequest request);
    void delete(Long id);

    List<ExamResponse> getUpcomingExams();
    List<ExamResponse> getTodaysExams();
    List<ExamResponse> getActiveExams();
    
    ExamDashboardResponse getDashboard();
}
