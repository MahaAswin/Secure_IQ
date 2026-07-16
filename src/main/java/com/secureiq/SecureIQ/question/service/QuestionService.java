package com.secureiq.SecureIQ.question.service;

import com.secureiq.SecureIQ.question.dto.*;
import com.secureiq.SecureIQ.question.model.Difficulty;
import com.secureiq.SecureIQ.question.model.QuestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface QuestionService {
    Page<QuestionResponse> getAll(Long bankId, Long subjectId, Long departmentId, Difficulty difficulty, QuestionType questionType, String keyword, Pageable pageable);
    QuestionResponse getById(Long id);
    QuestionResponse create(QuestionCreateRequest request);
    QuestionResponse update(Long id, QuestionUpdateRequest request);
    QuestionResponse patch(Long id, QuestionPatchRequest request);
    void delete(Long id);
    BulkImportResponse bulkImport(MultipartFile file, Long bankId);
    QuestionDashboardResponse getDashboard();
}
