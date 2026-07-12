package com.secureiq.SecureIQ.department.dto;

import com.secureiq.SecureIQ.user.dto.UserResponse;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {
    private Long id;
    private String departmentName;
    private String departmentCode;
    private String description;
    private UserResponse hod;
    private long totalStudents;
    private long totalFaculty;
    private String createdAt;
    private String updatedAt;
}
