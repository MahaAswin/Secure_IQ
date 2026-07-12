package com.secureiq.SecureIQ.subject.dto;

import com.secureiq.SecureIQ.department.dto.DepartmentResponse;
import com.secureiq.SecureIQ.user.dto.UserResponse;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectResponse {
    private Long id;
    private String subjectCode;
    private String subjectName;
    private String description;
    private Integer credits;
    private Integer semester;
    private String regulation;
    private DepartmentResponse department;
    private List<UserResponse> faculty;
    private String createdAt;
    private String updatedAt;
}
