package com.secureiq.SecureIQ.electron.dto;

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
public class ElectronDashboardResponse {
    // Faculty Dashboard metrics
    private Long connectedStudentsCount;
    private Long disconnectedStudentsCount;
    private List<Map<String, Object>> browserStatus;

    // Admin Dashboard metrics
    private Long activeBrowserSessionsCount;
    private Map<String, Long> browserVersionStatistics;
    private Map<String, Long> operatingSystemStatistics;
}
