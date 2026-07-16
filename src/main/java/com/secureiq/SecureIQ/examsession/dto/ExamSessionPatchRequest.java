package com.secureiq.SecureIQ.examsession.dto;

import com.secureiq.SecureIQ.examsession.model.SessionStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSessionPatchRequest {

    @Size(max = 50, message = "Session code must not exceed 50 characters")
    private String sessionCode;

    @Size(max = 100, message = "Session name must not exceed 100 characters")
    private String sessionName;

    private Long examId;

    private Long facultyId;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private String venue;

    private Integer joinedStudents;

    private SessionStatus status;

    private String instructions;
}
