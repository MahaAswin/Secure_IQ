package com.secureiq.SecureIQ.examattempt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TerminateAttemptRequest {

    @NotNull(message = "Attempt ID is required")
    private Long attemptId;

    @NotBlank(message = "Reason for termination is required")
    private String reason;
}
