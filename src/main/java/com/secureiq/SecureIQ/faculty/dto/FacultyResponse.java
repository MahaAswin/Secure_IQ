package com.secureiq.SecureIQ.faculty.dto;

import com.secureiq.SecureIQ.user.dto.UserResponse;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacultyResponse {
    private Long id;
    private UserResponse user;
    private String employeeId;
    private String designation;
    private String qualification;
    private String specialization;
    private Integer yearsOfExperience;
    private String officeLocation;
    private DepartmentDto department;
    private LocalDate joiningDate;
    private boolean profileCompleted;
    private List<SubjectDto> subjects;
    private String createdAt;
    private String updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentDto {
        private Long id;
        private String name;
        private String code;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectDto {
        private Long id;
        private String subjectCode;
        private String subjectName;
        private Integer credits;
        private Integer semester;
    }
}
