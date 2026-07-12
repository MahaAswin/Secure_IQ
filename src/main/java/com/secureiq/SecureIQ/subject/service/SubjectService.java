package com.secureiq.SecureIQ.subject.service;

import com.secureiq.SecureIQ.subject.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubjectService {
    Page<SubjectResponse> getAll(String subjectName, String subjectCode, Integer semester, Long departmentId, Pageable pageable);
    SubjectResponse getById(Long id);
    SubjectResponse create(SubjectCreateRequest request);
    SubjectResponse update(Long id, SubjectUpdateRequest request);
    SubjectResponse patch(Long id, SubjectPatchRequest request);
    void delete(Long id);
    SubjectDashboardResponse getDashboard();
}
