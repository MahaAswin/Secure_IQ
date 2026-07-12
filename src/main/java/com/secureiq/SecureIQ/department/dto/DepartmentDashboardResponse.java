package com.secureiq.SecureIQ.department.dto;

import com.secureiq.SecureIQ.user.dto.UserResponse;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDashboardResponse {
    private Long departmentId;
    private String departmentName;
    private String departmentCode;
    private String description;
    private long totalStudents;
    private long totalFaculty;
    private UserResponse hod;
}
