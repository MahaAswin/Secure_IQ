package com.secureiq.SecureIQ.questionbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankCreateRequest {

    @NotBlank(message = "Question bank name is required")
    @Size(max = 100, message = "Question bank name must not exceed 100 characters")
    private String bankName;

    private String description;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Department ID is required")
    private Long departmentId;
}
