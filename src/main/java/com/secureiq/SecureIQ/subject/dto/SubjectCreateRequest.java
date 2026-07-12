package com.secureiq.SecureIQ.subject.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectCreateRequest {

    @NotBlank(message = "Subject code is required")
    @Size(max = 50, message = "Subject code must not exceed 50 characters")
    private String subjectCode;

    @NotBlank(message = "Subject name is required")
    @Size(max = 100, message = "Subject name must not exceed 100 characters")
    private String subjectName;

    private String description;

    @NotNull(message = "Credits is required")
    @Positive(message = "Credits must be a positive number")
    private Integer credits;

    @NotNull(message = "Semester is required")
    @Min(value = 1, message = "Semester must be between 1 and 8")
    @Max(value = 8, message = "Semester must be between 1 and 8")
    private Integer semester;

    private String regulation;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    private List<Long> facultyIds;
}
