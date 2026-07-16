package com.secureiq.SecureIQ.examattempt.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartAttemptRequest {
    @NotNull(message = "Exam session ID is required")
    private Long examSessionId;
}
