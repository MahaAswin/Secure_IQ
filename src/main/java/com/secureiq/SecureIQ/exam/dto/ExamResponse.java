package com.secureiq.SecureIQ.exam.dto;

import com.secureiq.SecureIQ.exam.model.ExamType;
import com.secureiq.SecureIQ.exam.model.ExamStatus;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResponse {
    private Long id;
    private String examCode;
    private String examTitle;
    private String description;
    private SubjectDto subject;
    private FacultyDto faculty;
    private DepartmentDto department;
    private Integer semester;
    private ExamType examType;
    private Integer totalMarks;
    private Integer durationMinutes;
    private Integer passingMarks;
    private LocalDate scheduledDate;
    private LocalDate scheduledEndDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Instant scheduledStart;
    private Instant scheduledEnd;
    private Instant serverTime;
    private String scheduleState;
    private Boolean canStart;
    private String accessMessage;
    private String attemptStatus;
    private Long attemptId;
    private Long remainingSeconds;
    private String venue;
    private String instructions;
    private ExamStatus status;
    private String createdAt;
    private String updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectDto {
        private Long id;
        private String subjectCode;
        private String subjectName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacultyDto {
        private Long id;
        private String name;
        private String employeeId;
        private String designation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentDto {
        private Long id;
        private String name;
        private String code;
    }
}
