package com.secureiq.SecureIQ.electron.dto;

import com.secureiq.SecureIQ.examattempt.dto.StudentExamAttemptResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowserSessionResponse {
    private Long id;
    private String sessionId;
    private StudentExamAttemptResponse studentExamAttempt;
    private String browserVersion;
    private String operatingSystem;
    private String machineId;
    private String ipAddress;
    private String startedAt;
    private String endedAt;
    private boolean active;
    private String createdAt;
    private String updatedAt;
}
