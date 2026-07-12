package com.secureiq.SecureIQ.department.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentPatchRequest {

    @Size(max = 100, message = "Department name must not exceed 100 characters")
    private String departmentName;

    @Size(max = 50, message = "Department code must not exceed 50 characters")
    private String departmentCode;

    private String description;

    private Long hodId;
}
