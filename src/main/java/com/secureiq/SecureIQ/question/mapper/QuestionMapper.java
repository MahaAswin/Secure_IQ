package com.secureiq.SecureIQ.question.mapper;

import com.secureiq.SecureIQ.question.dto.QuestionCreateRequest;
import com.secureiq.SecureIQ.question.dto.QuestionPatchRequest;
import com.secureiq.SecureIQ.question.dto.QuestionResponse;
import com.secureiq.SecureIQ.question.dto.QuestionUpdateRequest;
import com.secureiq.SecureIQ.question.model.Question;
import com.secureiq.SecureIQ.questionbank.mapper.QuestionBankMapper;
import com.secureiq.SecureIQ.questionbank.model.QuestionBank;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class QuestionMapper {

    private final QuestionBankMapper questionBankMapper;

    public QuestionMapper(QuestionBankMapper questionBankMapper) {
        this.questionBankMapper = questionBankMapper;
    }

    public QuestionResponse toResponse(Question question) {
        if (question == null) {
            return null;
        }
        return QuestionResponse.builder()
                .id(question.getId())
                .questionText(question.getQuestionText())
                .questionType(question.getQuestionType())
                .difficulty(question.getDifficulty())
                .marks(question.getMarks())
                .options(question.getOptions() != null ? new ArrayList<>(question.getOptions()) : new ArrayList<>())
                .correctAnswer(question.getCorrectAnswer())
                .explanation(question.getExplanation())
                .imageUrl(question.getImageUrl())
                .bank(questionBankMapper.toResponse(question.getBank()))
                .createdAt(question.getCreatedAt() != null ? question.getCreatedAt().toString() : null)
                .updatedAt(question.getUpdatedAt() != null ? question.getUpdatedAt().toString() : null)
                .build();
    }

    public Question toEntity(QuestionCreateRequest request, QuestionBank bank) {
        if (request == null) {
            return null;
        }
        return Question.builder()
                .questionText(request.getQuestionText())
                .questionType(request.getQuestionType())
                .difficulty(request.getDifficulty())
                .marks(request.getMarks())
                .options(request.getOptions() != null ? new ArrayList<>(request.getOptions()) : new ArrayList<>())
                .correctAnswer(request.getCorrectAnswer())
                .explanation(request.getExplanation())
                .imageUrl(request.getImageUrl())
                .bank(bank)
                .deleted(false)
                .build();
    }

    public void updateEntity(QuestionUpdateRequest request, QuestionBank bank, Question question) {
        if (request == null || question == null) {
            return;
        }
        question.setQuestionText(request.getQuestionText());
        question.setQuestionType(request.getQuestionType());
        question.setDifficulty(request.getDifficulty());
        question.setMarks(request.getMarks());
        question.setOptions(request.getOptions() != null ? new ArrayList<>(request.getOptions()) : new ArrayList<>());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setExplanation(request.getExplanation());
        question.setImageUrl(request.getImageUrl());
        if (bank != null) {
            question.setBank(bank);
        }
    }

    public void patchEntity(QuestionPatchRequest request, QuestionBank bank, Question question) {
        if (request == null || question == null) {
            return;
        }
        if (request.getQuestionText() != null) {
            question.setQuestionText(request.getQuestionText());
        }
        if (request.getQuestionType() != null) {
            question.setQuestionType(request.getQuestionType());
        }
        if (request.getDifficulty() != null) {
            question.setDifficulty(request.getDifficulty());
        }
        if (request.getMarks() != null) {
            question.setMarks(request.getMarks());
        }
        if (request.getOptions() != null) {
            question.setOptions(new ArrayList<>(request.getOptions()));
        }
        if (request.getCorrectAnswer() != null) {
            question.setCorrectAnswer(request.getCorrectAnswer());
        }
        if (request.getExplanation() != null) {
            question.setExplanation(request.getExplanation());
        }
        if (request.getImageUrl() != null) {
            question.setImageUrl(request.getImageUrl());
        }
        if (bank != null) {
            question.setBank(bank);
        }
    }
}
