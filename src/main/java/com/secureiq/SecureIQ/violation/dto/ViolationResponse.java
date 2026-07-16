package com.secureiq.SecureIQ.violation.dto;

import com.secureiq.SecureIQ.examattempt.dto.StudentExamAttemptResponse;
import com.secureiq.SecureIQ.violation.model.Severity;
import com.secureiq.SecureIQ.violation.model.Source;
import com.secureiq.SecureIQ.violation.model.ViolationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationResponse {
    private Long id;
    private String violationCode;
    private StudentExamAttemptResponse studentExamAttempt;
    private ViolationType violationType;
    private Severity severity;
    private Source source;
    private String description;
    private Double confidenceScore;
    private String evidencePath;
    private String detectedAt;
    private String actionTaken;
    private boolean resolved;
    private String createdAt;
    private String updatedAt;
}
