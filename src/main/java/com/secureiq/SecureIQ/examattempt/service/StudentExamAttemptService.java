package com.secureiq.SecureIQ.examattempt.service;

import com.secureiq.SecureIQ.examattempt.dto.*;
import com.secureiq.SecureIQ.examattempt.model.AttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentExamAttemptService {
    Page<StudentExamAttemptResponse> getAll(String attemptCode, Long studentId, Long examSessionId, Long departmentId, AttemptStatus status, Pageable pageable);
    StudentExamAttemptResponse getById(Long id);
    StudentExamAttemptResponse startAttempt(StartAttemptRequest request);
    StudentExamAttemptResponse submitAttempt(SubmitAttemptRequest request);
    StudentExamAttemptResponse terminateAttempt(TerminateAttemptRequest request);
    Page<StudentExamAttemptResponse> getMyAttempts(String attemptCode, Long examSessionId, AttemptStatus status, Pageable pageable);
    Page<StudentExamAttemptResponse> getAttemptsBySession(Long sessionId, Pageable pageable);
    AttemptDashboardResponse getDashboard();
}
