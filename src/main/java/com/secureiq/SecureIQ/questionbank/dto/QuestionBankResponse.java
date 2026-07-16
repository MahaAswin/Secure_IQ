package com.secureiq.SecureIQ.questionbank.dto;

import com.secureiq.SecureIQ.department.dto.DepartmentResponse;
import com.secureiq.SecureIQ.subject.dto.SubjectResponse;
import com.secureiq.SecureIQ.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankResponse {
    private Long id;
    private String bankName;
    private String description;
    private SubjectResponse subject;
    private DepartmentResponse department;
    private UserResponse createdBy;
    private String createdAt;
    private String updatedAt;
}
