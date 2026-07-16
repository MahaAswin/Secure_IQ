package com.secureiq.SecureIQ.electron.mapper;

import com.secureiq.SecureIQ.electron.dto.BrowserSessionResponse;
import com.secureiq.SecureIQ.electron.model.BrowserSession;
import com.secureiq.SecureIQ.examattempt.mapper.StudentExamAttemptMapper;
import org.springframework.stereotype.Component;

@Component
public class BrowserSessionMapper {

    private final StudentExamAttemptMapper studentExamAttemptMapper;

    public BrowserSessionMapper(StudentExamAttemptMapper studentExamAttemptMapper) {
        this.studentExamAttemptMapper = studentExamAttemptMapper;
    }

    public BrowserSessionResponse toResponse(BrowserSession session) {
        if (session == null) {
            return null;
        }
        return BrowserSessionResponse.builder()
                .id(session.getId())
                .sessionId(session.getSessionId())
                .studentExamAttempt(studentExamAttemptMapper.toResponse(session.getStudentExamAttempt()))
                .browserVersion(session.getBrowserVersion())
                .operatingSystem(session.getOperatingSystem())
                .machineId(session.getMachineId())
                .ipAddress(session.getIpAddress())
                .startedAt(session.getStartedAt() != null ? session.getStartedAt().toString() : null)
                .endedAt(session.getEndedAt() != null ? session.getEndedAt().toString() : null)
                .active(session.isActive())
                .createdAt(session.getCreatedAt() != null ? session.getCreatedAt().toString() : null)
                .updatedAt(session.getUpdatedAt() != null ? session.getUpdatedAt().toString() : null)
                .build();
    }
}
