package com.secureiq.SecureIQ.examattempt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartAttemptRequest {
    private Long examSessionId;
    private Long examId;
    private Boolean resume;
}
