package com.secureiq.SecureIQ.examsession.mapper;

import com.secureiq.SecureIQ.exam.mapper.ExamMapper;
import com.secureiq.SecureIQ.exam.model.Exam;
import com.secureiq.SecureIQ.examsession.dto.ExamSessionCreateRequest;
import com.secureiq.SecureIQ.examsession.dto.ExamSessionPatchRequest;
import com.secureiq.SecureIQ.examsession.dto.ExamSessionResponse;
import com.secureiq.SecureIQ.examsession.dto.ExamSessionUpdateRequest;
import com.secureiq.SecureIQ.examsession.model.ExamSession;
import com.secureiq.SecureIQ.faculty.mapper.FacultyMapper;
import com.secureiq.SecureIQ.faculty.model.Faculty;
import org.springframework.stereotype.Component;

@Component
public class ExamSessionMapper {

    private final ExamMapper examMapper;
    private final FacultyMapper facultyMapper;

    public ExamSessionMapper(ExamMapper examMapper, FacultyMapper facultyMapper) {
        this.examMapper = examMapper;
        this.facultyMapper = facultyMapper;
    }

    public ExamSessionResponse toResponse(ExamSession session) {
        if (session == null) {
            return null;
        }
        return ExamSessionResponse.builder()
                .id(session.getId())
                .sessionCode(session.getSessionCode())
                .sessionName(session.getSessionName())
                .exam(examMapper.toResponse(session.getExam()))
                .faculty(facultyMapper.toResponse(session.getFaculty()))
                .startDateTime(session.getStartDateTime() != null ? session.getStartDateTime().toString() : null)
                .endDateTime(session.getEndDateTime() != null ? session.getEndDateTime().toString() : null)
                .venue(session.getVenue())
                .totalStudents(session.getTotalStudents())
                .joinedStudents(session.getJoinedStudents())
                .status(session.getStatus())
                .instructions(session.getInstructions())
                .createdAt(session.getCreatedAt() != null ? session.getCreatedAt().toString() : null)
                .updatedAt(session.getUpdatedAt() != null ? session.getUpdatedAt().toString() : null)
                .build();
    }

    public ExamSession toEntity(ExamSessionCreateRequest request, Exam exam, Faculty faculty) {
        if (request == null) {
            return null;
        }
        return ExamSession.builder()
                .sessionCode(request.getSessionCode())
                .sessionName(request.getSessionName())
                .exam(exam)
                .faculty(faculty)
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .venue(request.getVenue())
                .joinedStudents(request.getJoinedStudents() != null ? request.getJoinedStudents() : 0)
                .status(request.getStatus())
                .instructions(request.getInstructions())
                .totalStudents(0)
                .deleted(false)
                .build();
    }

    public void updateEntity(ExamSessionUpdateRequest request, Exam exam, Faculty faculty, ExamSession session) {
        if (request == null || session == null) {
            return;
        }
        session.setSessionCode(request.getSessionCode());
        session.setSessionName(request.getSessionName());
        session.setStartDateTime(request.getStartDateTime());
        session.setEndDateTime(request.getEndDateTime());
        session.setVenue(request.getVenue());
        session.setStatus(request.getStatus());
        session.setInstructions(request.getInstructions());
        if (request.getJoinedStudents() != null) {
            session.setJoinedStudents(request.getJoinedStudents());
        }
        if (exam != null) {
            session.setExam(exam);
        }
        if (faculty != null) {
            session.setFaculty(faculty);
        }
    }

    public void patchEntity(ExamSessionPatchRequest request, Exam exam, Faculty faculty, ExamSession session) {
        if (request == null || session == null) {
            return;
        }
        if (request.getSessionCode() != null) {
            session.setSessionCode(request.getSessionCode());
        }
        if (request.getSessionName() != null) {
            session.setSessionName(request.getSessionName());
        }
        if (request.getStartDateTime() != null) {
            session.setStartDateTime(request.getStartDateTime());
        }
        if (request.getEndDateTime() != null) {
            session.setEndDateTime(request.getEndDateTime());
        }
        if (request.getVenue() != null) {
            session.setVenue(request.getVenue());
        }
        if (request.getJoinedStudents() != null) {
            session.setJoinedStudents(request.getJoinedStudents());
        }
        if (request.getStatus() != null) {
            session.setStatus(request.getStatus());
        }
        if (request.getInstructions() != null) {
            session.setInstructions(request.getInstructions());
        }
        if (exam != null) {
            session.setExam(exam);
        }
        if (faculty != null) {
            session.setFaculty(faculty);
        }
    }
}
