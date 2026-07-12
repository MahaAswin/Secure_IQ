package com.secureiq.SecureIQ.subject.dto;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDashboardResponse {
    private long totalSubjects;
    private Map<Integer, Long> subjectsBySemester;
    private Map<String, Long> subjectsByDepartment;
}
