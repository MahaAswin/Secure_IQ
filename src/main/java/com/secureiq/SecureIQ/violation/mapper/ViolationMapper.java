package com.secureiq.SecureIQ.violation.mapper;

import com.secureiq.SecureIQ.examattempt.mapper.StudentExamAttemptMapper;
import com.secureiq.SecureIQ.examattempt.model.StudentExamAttempt;
import com.secureiq.SecureIQ.violation.dto.ViolationCreateRequest;
import com.secureiq.SecureIQ.violation.dto.ViolationResponse;
import com.secureiq.SecureIQ.violation.dto.ViolationUpdateRequest;
import com.secureiq.SecureIQ.violation.model.Violation;
import org.springframework.stereotype.Component;

@Component
public class ViolationMapper {

    private final StudentExamAttemptMapper studentExamAttemptMapper;

    public ViolationMapper(StudentExamAttemptMapper studentExamAttemptMapper) {
        this.studentExamAttemptMapper = studentExamAttemptMapper;
    }

    public ViolationResponse toResponse(Violation violation) {
        if (violation == null) {
            return null;
        }
        return ViolationResponse.builder()
                .id(violation.getId())
                .violationCode(violation.getViolationCode())
                .studentExamAttempt(studentExamAttemptMapper.toResponse(violation.getStudentExamAttempt()))
                .violationType(violation.getViolationType())
                .severity(violation.getSeverity())
                .source(violation.getSource())
                .description(violation.getDescription())
                .confidenceScore(violation.getConfidenceScore())
                .evidencePath(violation.getEvidencePath())
                .detectedAt(violation.getDetectedAt() != null ? violation.getDetectedAt().toString() : null)
                .actionTaken(violation.getActionTaken())
                .resolved(violation.isResolved())
                .createdAt(violation.getCreatedAt() != null ? violation.getCreatedAt().toString() : null)
                .updatedAt(violation.getUpdatedAt() != null ? violation.getUpdatedAt().toString() : null)
                .build();
    }

    public Violation toEntity(ViolationCreateRequest request, StudentExamAttempt attempt) {
        if (request == null) {
            return null;
        }
        return Violation.builder()
                .violationCode(request.getViolationCode())
                .studentExamAttempt(attempt)
                .violationType(request.getViolationType())
                .severity(request.getSeverity())
                .source(request.getSource())
                .description(request.getDescription())
                .confidenceScore(request.getConfidenceScore())
                .evidencePath(request.getEvidencePath())
                .detectedAt(request.getDetectedAt())
                .actionTaken(request.getActionTaken())
                .resolved(request.getResolved() != null ? request.getResolved() : false)
                .deleted(false)
                .build();
    }

    public void updateEntity(ViolationUpdateRequest request, StudentExamAttempt attempt, Violation violation) {
        if (request == null || violation == null) {
            return;
        }
        if (request.getViolationCode() != null) {
            violation.setViolationCode(request.getViolationCode());
        }
        if (request.getViolationType() != null) {
            violation.setViolationType(request.getViolationType());
        }
        if (request.getSeverity() != null) {
            violation.setSeverity(request.getSeverity());
        }
        if (request.getSource() != null) {
            violation.setSource(request.getSource());
        }
        if (request.getDescription() != null) {
            violation.setDescription(request.getDescription());
        }
        if (request.getConfidenceScore() != null) {
            violation.setConfidenceScore(request.getConfidenceScore());
        }
        if (request.getEvidencePath() != null) {
            violation.setEvidencePath(request.getEvidencePath());
        }
        if (request.getDetectedAt() != null) {
            violation.setDetectedAt(request.getDetectedAt());
        }
        if (request.getActionTaken() != null) {
            violation.setActionTaken(request.getActionTaken());
        }
        if (request.getResolved() != null) {
            violation.setResolved(request.getResolved());
        }
        if (attempt != null) {
            violation.setStudentExamAttempt(attempt);
        }
    }
}
