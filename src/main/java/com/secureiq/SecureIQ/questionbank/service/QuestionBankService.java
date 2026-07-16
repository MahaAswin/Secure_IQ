package com.secureiq.SecureIQ.questionbank.service;

import com.secureiq.SecureIQ.questionbank.dto.QuestionBankCreateRequest;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankPatchRequest;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankResponse;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionBankService {
    Page<QuestionBankResponse> getAll(String bankName, Long subjectId, Long departmentId, Pageable pageable);
    QuestionBankResponse getById(Long id);
    QuestionBankResponse create(QuestionBankCreateRequest request);
    QuestionBankResponse update(Long id, QuestionBankUpdateRequest request);
    QuestionBankResponse patch(Long id, QuestionBankPatchRequest request);
    void delete(Long id);
}
