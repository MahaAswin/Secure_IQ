package com.secureiq.SecureIQ.student.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPatchRequest {

    @Size(max = 50, message = "Register number must not exceed 50 characters")
    private String registerNumber;

    @Size(max = 50, message = "Roll number must not exceed 50 characters")
    private String rollNumber;

    private Long departmentId;

    @Size(max = 20, message = "Academic year must not exceed 20 characters")
    private String academicYear;

    private Integer semester;

    private String section;

    private String batch;

    private LocalDate dateOfBirth;

    private String gender;

    private String address;

    private String parentName;

    @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$", message = "Invalid parent phone number format")
    private String parentPhone;

    private String emergencyContact;

    private Boolean profileCompleted;
}
