package com.secureiq.SecureIQ.examsession.service;

import com.secureiq.SecureIQ.examsession.dto.*;
import com.secureiq.SecureIQ.examsession.model.SessionStatus;
import com.secureiq.SecureIQ.student.dto.StudentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ExamSessionService {
    Page<ExamSessionResponse> getAll(String sessionCode, Long examId, Long facultyId, Long departmentId, SessionStatus status, Pageable pageable);
    ExamSessionResponse getById(Long id);
    ExamSessionResponse create(ExamSessionCreateRequest request);
    ExamSessionResponse update(Long id, ExamSessionUpdateRequest request);
    ExamSessionResponse patch(Long id, ExamSessionPatchRequest request);
    void delete(Long id);
    ExamSessionResponse assignStudents(Long sessionId, StudentAssignmentRequest request);
    List<StudentResponse> getAssignedStudents(Long sessionId);
    List<ExamSessionResponse> getLiveSessions();
    List<ExamSessionResponse> getUpcomingSessions();
    ExamSessionDashboardResponse getDashboard();
}
