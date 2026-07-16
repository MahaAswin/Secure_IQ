package com.secureiq.SecureIQ.violation.dto;

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
public class ViolationDashboardResponse {
    // Student Dashboard metrics
    private Long totalViolationsCount;

    // Faculty Dashboard metrics
    private Long liveViolationsCount;
    private Long criticalViolationsCount;

    // Admin Dashboard metrics
    private Map<String, Long> severityStatistics;
    private Map<String, Long> typeStatistics;
    private List<Map<String, Object>> dailyReports;
}
