package com.secureiq.SecureIQ.examattempt.dto;

import com.secureiq.SecureIQ.examattempt.model.AttemptStatus;
import com.secureiq.SecureIQ.examsession.dto.ExamSessionResponse;
import com.secureiq.SecureIQ.student.dto.StudentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExamAttemptResponse {
    private Long id;
    private String attemptCode;
    private StudentResponse student;
    private ExamSessionResponse examSession;
    private String startTime;
    private String endTime;
    private String submittedTime;
    private AttemptStatus status;
    private Double score;
    private Double percentage;
    private Integer totalMarks;
    private Double obtainedMarks;
    private boolean autoSubmitted;
    private Integer browserWarnings;
    private Integer aiWarnings;
    private Integer totalViolations;
    private String createdAt;
    private String updatedAt;
}
