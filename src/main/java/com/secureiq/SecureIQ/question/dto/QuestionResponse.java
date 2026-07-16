package com.secureiq.SecureIQ.question.dto;

import com.secureiq.SecureIQ.question.model.Difficulty;
import com.secureiq.SecureIQ.question.model.QuestionType;
import com.secureiq.SecureIQ.questionbank.dto.QuestionBankResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {
    private Long id;
    private String questionText;
    private QuestionType questionType;
    private Difficulty difficulty;
    private Integer marks;
    private List<String> options;
    private String correctAnswer;
    private String explanation;
    private String imageUrl;
    private QuestionBankResponse bank;
    private String createdAt;
    private String updatedAt;
}
