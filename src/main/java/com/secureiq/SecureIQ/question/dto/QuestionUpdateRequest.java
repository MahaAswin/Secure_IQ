package com.secureiq.SecureIQ.question.dto;

import com.secureiq.SecureIQ.question.model.Difficulty;
import com.secureiq.SecureIQ.question.model.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class QuestionUpdateRequest {

    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotNull(message = "Question type is required")
    private QuestionType questionType;

    @NotNull(message = "Difficulty is required")
    private Difficulty difficulty;

    @NotNull(message = "Marks is required")
    @Positive(message = "Marks must be a positive number")
    private Integer marks;

    private List<String> options;

    @NotBlank(message = "Correct answer is required")
    private String correctAnswer;

    private String explanation;

    private String imageUrl;

    @NotNull(message = "Question bank ID is required")
    private Long bankId;
}
