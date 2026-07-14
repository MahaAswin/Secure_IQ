package com.secureiq.SecureIQ.faculty.dto;

import com.secureiq.SecureIQ.student.dto.RecentActivityResponse;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacultyDashboardResponse {
    private FacultyResponse facultyProfile;
    private long assignedSubjectsCount;
    private long assignedStudentsCount;
    private long upcomingExamsCount;
    private List<RecentActivityResponse> recentActivities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExamDto {
        private Long id;
        private String title;
        private String scheduledAt;
        private String departmentName;
    }
}
