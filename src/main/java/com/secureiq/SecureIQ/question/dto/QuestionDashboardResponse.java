package com.secureiq.SecureIQ.question.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDashboardResponse {
    private Long totalQuestionBanks;
    private Long totalQuestions;
    private Map<String, Long> questionsBySubject;
    private Map<String, Long> questionsByDifficulty;
}
