package com.secureiq.SecureIQ.questionbank.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankPatchRequest {

    @Size(max = 100, message = "Question bank name must not exceed 100 characters")
    private String bankName;

    private String description;

    private Long subjectId;

    private Long departmentId;
}
