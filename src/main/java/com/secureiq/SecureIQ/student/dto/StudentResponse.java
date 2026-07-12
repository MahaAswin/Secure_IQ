package com.secureiq.SecureIQ.student.dto;

import com.secureiq.SecureIQ.user.dto.UserResponse;
import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    private Long id;
    private UserResponse user;
    private String registerNumber;
    private String rollNumber;
    private DepartmentDto department;
    private String academicYear;
    private Integer semester;
    private String section;
    private String batch;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private String parentName;
    private String parentPhone;
    private String emergencyContact;
    private boolean profileCompleted;
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
}
