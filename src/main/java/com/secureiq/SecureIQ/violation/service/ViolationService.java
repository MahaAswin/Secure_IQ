package com.secureiq.SecureIQ.violation.service;

import com.secureiq.SecureIQ.violation.dto.*;
import com.secureiq.SecureIQ.violation.model.Severity;
import com.secureiq.SecureIQ.violation.model.Source;
import com.secureiq.SecureIQ.violation.model.ViolationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ViolationService {
    Page<ViolationResponse> getAll(String violationCode, Long studentId, Long sessionId, Severity severity, ViolationType type, Source source, Pageable pageable);
    ViolationResponse getById(Long id);
    ViolationResponse recordViolation(ViolationCreateRequest request);
    ViolationResponse updateViolation(Long id, ViolationUpdateRequest request);
    Page<ViolationResponse> getViolationsByStudent(Long studentId, Pageable pageable);
    Page<ViolationResponse> getViolationsBySession(Long sessionId, Pageable pageable);
    Page<ViolationResponse> getLiveViolations(Pageable pageable);
    ViolationDashboardResponse getDashboard();
}
