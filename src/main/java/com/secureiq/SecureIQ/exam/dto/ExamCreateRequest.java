package com.secureiq.SecureIQ.exam.dto;

import com.secureiq.SecureIQ.exam.model.ExamType;
import com.secureiq.SecureIQ.exam.model.ExamStatus;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamCreateRequest {

    @NotBlank(message = "Exam code is required")
    @Size(max = 50, message = "Exam code must not exceed 50 characters")
    private String examCode;

    @NotBlank(message = "Exam title is required")
    @Size(max = 100, message = "Exam title must not exceed 100 characters")
    private String examTitle;

    private String description;

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    @NotNull(message = "Faculty ID is required")
    private Long facultyId;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotNull(message = "Semester is required")
    @Min(value = 1, message = "Semester must be at least 1")
    private Integer semester;

    @NotNull(message = "Exam type is required")
    private ExamType examType;

    @NotNull(message = "Total marks is required")
    @Min(value = 1, message = "Total marks must be positive")
    private Integer totalMarks;

    @NotNull(message = "Duration in minutes is required")
    @Min(value = 1, message = "Duration must be positive")
    private Integer durationMinutes;

    @NotNull(message = "Passing marks is required")
    @Min(value = 0, message = "Passing marks cannot be negative")
    private Integer passingMarks;

    @NotNull(message = "Scheduled date is required")
    @FutureOrPresent(message = "Scheduled date cannot be in the past")
    private LocalDate scheduledDate;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    private String venue;

    private String instructions;

    private ExamStatus status;
}
