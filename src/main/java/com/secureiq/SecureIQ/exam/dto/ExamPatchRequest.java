package com.secureiq.SecureIQ.exam.dto;

import com.secureiq.SecureIQ.exam.model.ExamType;
import com.secureiq.SecureIQ.exam.model.ExamStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamPatchRequest {

    @Size(max = 50, message = "Exam code must not exceed 50 characters")
    private String examCode;

    @Size(max = 100, message = "Exam title must not exceed 100 characters")
    private String examTitle;

    private String description;

    private Long subjectId;

    private Long facultyId;

    private Long departmentId;

    @Min(value = 1, message = "Semester must be at least 1")
    private Integer semester;

    private ExamType examType;

    @Min(value = 1, message = "Total marks must be positive")
    private Integer totalMarks;

    @Min(value = 1, message = "Duration must be positive")
    private Integer durationMinutes;

    @Min(value = 0, message = "Passing marks cannot be negative")
    private Integer passingMarks;

    @FutureOrPresent(message = "Scheduled date cannot be in the past")
    private LocalDate scheduledDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private String venue;

    private String instructions;

    private ExamStatus status;
}
