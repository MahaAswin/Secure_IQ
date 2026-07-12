package com.secureiq.SecureIQ.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentUpdateRequest {

    @NotBlank(message = "Department name is required")
    @Size(max = 100, message = "Department name must not exceed 100 characters")
    private String departmentName;

    @NotBlank(message = "Department code is required")
    @Size(max = 50, message = "Department code must not exceed 50 characters")
    private String departmentCode;

    private String description;

    private Long hodId;
}
