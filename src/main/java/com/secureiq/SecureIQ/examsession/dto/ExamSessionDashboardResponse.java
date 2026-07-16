package com.secureiq.SecureIQ.examsession.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSessionDashboardResponse {
    // Student Dashboard metrics
    private List<ExamSessionResponse> upcomingSessions;
    private ExamSessionResponse activeSession;

    // Faculty Dashboard metrics
    private List<ExamSessionResponse> mySessions;
    private List<ExamSessionResponse> liveSessions;

    // Admin Dashboard metrics
    private Long activeSessionsCount;
    private Long totalSessionsCount;
    private Map<String, Object> attendanceSummary;
}
