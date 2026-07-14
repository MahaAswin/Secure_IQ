package com.secureiq.SecureIQ.exam.dto;

import com.secureiq.SecureIQ.exam.model.ExamType;
import com.secureiq.SecureIQ.exam.model.ExamStatus;
import lombok.*;
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
    private LocalTime startTime;
    private LocalTime endTime;
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
