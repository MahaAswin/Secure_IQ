package com.secureiq.SecureIQ.examattempt.mapper;

import com.secureiq.SecureIQ.examattempt.dto.StudentExamAttemptResponse;
import com.secureiq.SecureIQ.examattempt.model.StudentExamAttempt;
import com.secureiq.SecureIQ.examsession.mapper.ExamSessionMapper;
import com.secureiq.SecureIQ.student.mapper.StudentMapper;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class StudentExamAttemptMapper {

    private final StudentMapper studentMapper;
    private final ExamSessionMapper examSessionMapper;
    private final Clock clock;

    public StudentExamAttemptMapper(StudentMapper studentMapper, ExamSessionMapper examSessionMapper, Clock clock) {
        this.studentMapper = studentMapper;
        this.examSessionMapper = examSessionMapper;
        this.clock = clock;
    }

    public StudentExamAttemptResponse toResponse(StudentExamAttempt attempt) {
        if (attempt == null) {
            return null;
        }

        Instant now = clock.instant();
        Long remainingSeconds = null;
        if (attempt.getStatus() == com.secureiq.SecureIQ.examattempt.model.AttemptStatus.IN_PROGRESS && attempt.getEndTime() != null) {
            Instant endInstant = attempt.getAuthoritativeEndTime();
            if (endInstant != null) {
                remainingSeconds = Math.max(0L, Duration.between(now, endInstant).toSeconds());
            }
        }

        Instant deadlineInstant = attempt.getAuthoritativeEndTime();

        return StudentExamAttemptResponse.builder()
                .id(attempt.getId())
                .attemptCode(attempt.getAttemptCode())
                .student(studentMapper.toResponse(attempt.getStudent()))
                .examSession(examSessionMapper.toResponse(attempt.getExamSession()))
                .startTime(attempt.getStartTime() != null ? attempt.getStartTime().toString() : null)
                .endTime(attempt.getEndTime() != null ? attempt.getEndTime().toString() : null)
                .submittedTime(attempt.getSubmittedTime() != null ? attempt.getSubmittedTime().toString() : null)
                .status(attempt.getStatus())
                .score(attempt.getScore())
                .percentage(attempt.getPercentage())
                .totalMarks(attempt.getTotalMarks())
                .obtainedMarks(attempt.getObtainedMarks())
                .autoSubmitted(attempt.isAutoSubmitted())
                .browserWarnings(attempt.getBrowserWarnings())
                .aiWarnings(attempt.getAiWarnings())
                .totalViolations(attempt.getTotalViolations())
                .remainingSeconds(remainingSeconds)
                .serverTime(now.toString())
                .effectiveDeadline(deadlineInstant != null ? deadlineInstant.toString() : null)
                .createdAt(attempt.getCreatedAt() != null ? attempt.getCreatedAt().toString() : null)
                .updatedAt(attempt.getUpdatedAt() != null ? attempt.getUpdatedAt().toString() : null)
                .build();
    }
}
