package com.secureiq.SecureIQ.violation.dto;

import com.secureiq.SecureIQ.violation.model.Severity;
import com.secureiq.SecureIQ.violation.model.Source;
import com.secureiq.SecureIQ.violation.model.ViolationType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationUpdateRequest {

    @Size(max = 100, message = "Violation code must not exceed 100 characters")
    private String violationCode;

    private Long studentExamAttemptId;

    private ViolationType violationType;

    private Severity severity;

    private Source source;

    private String description;

    @Min(value = 0, message = "Confidence score must be at least 0")
    @Max(value = 100, message = "Confidence score must not exceed 100")
    private Double confidenceScore;

    private String evidencePath;

    private LocalDateTime detectedAt;

    private String actionTaken;

    private Boolean resolved;
}
