package com.secureiq.SecureIQ.examsession.dto;

import com.secureiq.SecureIQ.examsession.model.SessionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSessionCreateRequest {

    @NotBlank(message = "Session code is required")
    @Size(max = 50, message = "Session code must not exceed 50 characters")
    private String sessionCode;

    @NotBlank(message = "Session name is required")
    @Size(max = 100, message = "Session name must not exceed 100 characters")
    private String sessionName;

    @NotNull(message = "Exam ID is required")
    private Long examId;

    @NotNull(message = "Faculty ID is required")
    private Long facultyId;

    @NotNull(message = "Start date time is required")
    private LocalDateTime startDateTime;

    @NotNull(message = "End date time is required")
    private LocalDateTime endDateTime;

    @NotBlank(message = "Venue is required")
    private String venue;

    private Integer joinedStudents;

    @NotNull(message = "Status is required")
    private SessionStatus status;

    private String instructions;
}
