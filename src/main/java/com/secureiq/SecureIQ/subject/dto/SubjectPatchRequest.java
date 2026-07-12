package com.secureiq.SecureIQ.subject.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectPatchRequest {

    @Size(max = 50, message = "Subject code must not exceed 50 characters")
    private String subjectCode;

    @Size(max = 100, message = "Subject name must not exceed 100 characters")
    private String subjectName;

    private String description;

    @Positive(message = "Credits must be a positive number")
    private Integer credits;

    @Min(value = 1, message = "Semester must be between 1 and 8")
    @Max(value = 8, message = "Semester must be between 1 and 8")
    private Integer semester;

    private String regulation;

    private Long departmentId;

    private List<Long> facultyIds;
}
