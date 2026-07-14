package com.secureiq.SecureIQ.exam.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamDashboardResponse {
    private String role;
    
    // Student Dashboard metrics
    private List<ExamResponse> upcomingExams;
    private List<ExamResponse> todaysExams;

    // Faculty Dashboard metrics
    private List<ExamResponse> createdExams;
    private List<ExamResponse> scheduledExams;

    // Admin Dashboard metrics
    private Long totalExams;
    private Long activeExamsCount;
    private Long completedExamsCount;
    private Map<String, Long> examsByType;
    private Map<String, Long> examsByStatus;
}
