package com.secureiq.SecureIQ.exam.mapper;

import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.exam.dto.ExamCreateRequest;
import com.secureiq.SecureIQ.exam.dto.ExamPatchRequest;
import com.secureiq.SecureIQ.exam.dto.ExamResponse;
import com.secureiq.SecureIQ.exam.dto.ExamUpdateRequest;
import com.secureiq.SecureIQ.exam.model.Exam;
import com.secureiq.SecureIQ.exam.model.ExamStatus;
import com.secureiq.SecureIQ.faculty.model.Faculty;
import com.secureiq.SecureIQ.subject.model.Subject;
import org.springframework.stereotype.Component;

@Component
public class ExamMapper {

    public ExamResponse toResponse(Exam exam) {
        if (exam == null) {
            return null;
        }

        ExamResponse.SubjectDto subjectDto = null;
        if (exam.getSubject() != null) {
            subjectDto = ExamResponse.SubjectDto.builder()
                    .id(exam.getSubject().getId())
                    .subjectCode(exam.getSubject().getSubjectCode())
                    .subjectName(exam.getSubject().getSubjectName())
                    .build();
        }

        ExamResponse.FacultyDto facultyDto = null;
        if (exam.getFaculty() != null) {
            String name = "";
            if (exam.getFaculty().getUser() != null) {
                name = exam.getFaculty().getUser().getFirstName() + " " + exam.getFaculty().getUser().getLastName();
            }
            facultyDto = ExamResponse.FacultyDto.builder()
                    .id(exam.getFaculty().getId())
                    .name(name)
                    .employeeId(exam.getFaculty().getEmployeeId())
                    .designation(exam.getFaculty().getDesignation())
                    .build();
        }

        ExamResponse.DepartmentDto departmentDto = null;
        if (exam.getDepartment() != null) {
            departmentDto = ExamResponse.DepartmentDto.builder()
                    .id(exam.getDepartment().getId())
                    .name(exam.getDepartment().getDepartmentName())
                    .code(exam.getDepartment().getDepartmentCode())
                    .build();
        }

        return ExamResponse.builder()
                .id(exam.getId())
                .examCode(exam.getExamCode())
                .examTitle(exam.getExamTitle())
                .description(exam.getDescription())
                .subject(subjectDto)
                .faculty(facultyDto)
                .department(departmentDto)
                .semester(exam.getSemester())
                .examType(exam.getExamType())
                .totalMarks(exam.getTotalMarks())
                .durationMinutes(exam.getDurationMinutes())
                .passingMarks(exam.getPassingMarks())
                .scheduledDate(exam.getScheduledDate())
                .startTime(exam.getStartTime())
                .endTime(exam.getEndTime())
                .venue(exam.getVenue())
                .instructions(exam.getInstructions())
                .status(exam.getStatus())
                .createdAt(exam.getCreatedAt() != null ? exam.getCreatedAt().toString() : null)
                .updatedAt(exam.getUpdatedAt() != null ? exam.getUpdatedAt().toString() : null)
                .build();
    }

    public Exam toEntity(ExamCreateRequest request, Subject subject, Faculty faculty, Department department) {
        if (request == null) {
            return null;
        }

        return Exam.builder()
                .examCode(request.getExamCode())
                .examTitle(request.getExamTitle())
                .description(request.getDescription())
                .subject(subject)
                .faculty(faculty)
                .department(department)
                .semester(request.getSemester())
                .examType(request.getExamType())
                .totalMarks(request.getTotalMarks())
                .durationMinutes(request.getDurationMinutes())
                .passingMarks(request.getPassingMarks())
                .scheduledDate(request.getScheduledDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .venue(request.getVenue())
                .instructions(request.getInstructions())
                .status(request.getStatus() != null ? request.getStatus() : ExamStatus.DRAFT)
                .deleted(false)
                .build();
    }

    public void updateEntity(ExamUpdateRequest request, Subject subject, Faculty faculty, Department department, Exam exam) {
        if (request == null || exam == null) {
            return;
        }
        exam.setExamCode(request.getExamCode());
        exam.setExamTitle(request.getExamTitle());
        exam.setDescription(request.getDescription());
        exam.setSubject(subject);
        exam.setFaculty(faculty);
        exam.setDepartment(department);
        exam.setSemester(request.getSemester());
        exam.setExamType(request.getExamType());
        exam.setTotalMarks(request.getTotalMarks());
        exam.setDurationMinutes(request.getDurationMinutes());
        exam.setPassingMarks(request.getPassingMarks());
        exam.setScheduledDate(request.getScheduledDate());
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        exam.setVenue(request.getVenue());
        exam.setInstructions(request.getInstructions());
        exam.setStatus(request.getStatus());
    }

    public void patchEntity(ExamPatchRequest request, Subject subject, Faculty faculty, Department department, Exam exam) {
        if (request == null || exam == null) {
            return;
        }
        if (request.getExamCode() != null) {
            exam.setExamCode(request.getExamCode());
        }
        if (request.getExamTitle() != null) {
            exam.setExamTitle(request.getExamTitle());
        }
        if (request.getDescription() != null) {
            exam.setDescription(request.getDescription());
        }
        if (subject != null) {
            exam.setSubject(subject);
        }
        if (faculty != null) {
            exam.setFaculty(faculty);
        }
        if (department != null) {
            exam.setDepartment(department);
        }
        if (request.getSemester() != null) {
            exam.setSemester(request.getSemester());
        }
        if (request.getExamType() != null) {
            exam.setExamType(request.getExamType());
        }
        if (request.getTotalMarks() != null) {
            exam.setTotalMarks(request.getTotalMarks());
        }
        if (request.getDurationMinutes() != null) {
            exam.setDurationMinutes(request.getDurationMinutes());
        }
        if (request.getPassingMarks() != null) {
            exam.setPassingMarks(request.getPassingMarks());
        }
        if (request.getScheduledDate() != null) {
            exam.setScheduledDate(request.getScheduledDate());
        }
        if (request.getStartTime() != null) {
            exam.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            exam.setEndTime(request.getEndTime());
        }
        if (request.getVenue() != null) {
            exam.setVenue(request.getVenue());
        }
        if (request.getInstructions() != null) {
            exam.setInstructions(request.getInstructions());
        }
        if (request.getStatus() != null) {
            exam.setStatus(request.getStatus());
        }
    }
}
