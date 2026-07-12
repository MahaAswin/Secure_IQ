package com.secureiq.SecureIQ.student.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDashboardResponse {
    private StudentResponse studentProfile;
    private long upcomingExamsCount;
    private long completedExamsCount;
    private long notificationsCount;
    private List<RecentActivityResponse> recentActivities;
}
