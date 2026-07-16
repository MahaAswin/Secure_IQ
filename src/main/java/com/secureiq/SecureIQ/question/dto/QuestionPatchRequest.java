package com.secureiq.SecureIQ.question.dto;

import com.secureiq.SecureIQ.question.model.Difficulty;
import com.secureiq.SecureIQ.question.model.QuestionType;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionPatchRequest {

    private String questionText;

    private QuestionType questionType;

    private Difficulty difficulty;

    @Positive(message = "Marks must be a positive number")
    private Integer marks;

    private List<String> options;

    private String correctAnswer;

    private String explanation;

    private String imageUrl;

    private Long bankId;
}
