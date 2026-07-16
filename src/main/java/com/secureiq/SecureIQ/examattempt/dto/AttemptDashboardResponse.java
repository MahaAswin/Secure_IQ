package com.secureiq.SecureIQ.examattempt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptDashboardResponse {
    // Student Dashboard metrics
    private StudentExamAttemptResponse currentAttempt;
    private Long remainingTimeSeconds;
    private String attemptStatus;

    // Faculty Dashboard metrics
    private Long activeAttemptsCount;
    private Long submittedAttemptsCount;

    // Admin Dashboard metrics
    private Long totalAttemptsCount;
    private Long liveAttemptsCount;
}
