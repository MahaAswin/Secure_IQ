package com.secureiq.SecureIQ.examsession.dto;

import com.secureiq.SecureIQ.exam.dto.ExamResponse;
import com.secureiq.SecureIQ.examsession.model.SessionStatus;
import com.secureiq.SecureIQ.faculty.dto.FacultyResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSessionResponse {
    private Long id;
    private String sessionCode;
    private String sessionName;
    private ExamResponse exam;
    private FacultyResponse faculty;
    private String startDateTime;
    private String endDateTime;
    private String venue;
    private Integer totalStudents;
    private Integer joinedStudents;
    private SessionStatus status;
    private String instructions;
    private String createdAt;
    private String updatedAt;
}
