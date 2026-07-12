package com.secureiq.SecureIQ.subject.mapper;

import com.secureiq.SecureIQ.department.mapper.DepartmentMapper;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.subject.dto.SubjectCreateRequest;
import com.secureiq.SecureIQ.subject.dto.SubjectPatchRequest;
import com.secureiq.SecureIQ.subject.dto.SubjectResponse;
import com.secureiq.SecureIQ.subject.dto.SubjectUpdateRequest;
import com.secureiq.SecureIQ.subject.model.Subject;
import com.secureiq.SecureIQ.user.mapper.UserMapper;
import com.secureiq.SecureIQ.user.model.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SubjectMapper {

    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;

    public SubjectMapper(DepartmentMapper departmentMapper, UserMapper userMapper) {
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
    }

    public SubjectResponse toResponse(Subject subject) {
        if (subject == null) {
            return null;
        }

        List<com.secureiq.SecureIQ.user.dto.UserResponse> facultyList = new ArrayList<>();
        if (subject.getFaculty() != null) {
            facultyList = subject.getFaculty().stream()
                    .map(userMapper::toResponse)
                    .collect(Collectors.toList());
        }

        return SubjectResponse.builder()
                .id(subject.getId())
                .subjectCode(subject.getSubjectCode())
                .subjectName(subject.getSubjectName())
                .description(subject.getDescription())
                .credits(subject.getCredits())
                .semester(subject.getSemester())
                .regulation(subject.getRegulation())
                .department(departmentMapper.toResponse(subject.getDepartment(), 0, 0))
                .faculty(facultyList)
                .createdAt(subject.getCreatedAt() != null ? subject.getCreatedAt().toString() : null)
                .updatedAt(subject.getUpdatedAt() != null ? subject.getUpdatedAt().toString() : null)
                .build();
    }

    public Subject toEntity(SubjectCreateRequest request, Department department, Set<User> faculty) {
        if (request == null) {
            return null;
        }

        return Subject.builder()
                .subjectCode(request.getSubjectCode())
                .subjectName(request.getSubjectName())
                .description(request.getDescription())
                .credits(request.getCredits())
                .semester(request.getSemester())
                .regulation(request.getRegulation())
                .department(department)
                .faculty(faculty != null ? faculty : new HashSet<>())
                .deleted(false)
                .build();
    }

    public void updateEntity(SubjectUpdateRequest request, Department department, Set<User> faculty, Subject subject) {
        if (request == null || subject == null) {
            return;
        }
        subject.setSubjectCode(request.getSubjectCode());
        subject.setSubjectName(request.getSubjectName());
        subject.setDescription(request.getDescription());
        subject.setCredits(request.getCredits());
        subject.setSemester(request.getSemester());
        subject.setRegulation(request.getRegulation());
        subject.setDepartment(department);
        if (faculty != null) {
            subject.setFaculty(faculty);
        }
    }

    public void patchEntity(SubjectPatchRequest request, Department department, Set<User> faculty, Subject subject) {
        if (request == null || subject == null) {
            return;
        }
        if (request.getSubjectCode() != null) {
            subject.setSubjectCode(request.getSubjectCode());
        }
        if (request.getSubjectName() != null) {
            subject.setSubjectName(request.getSubjectName());
        }
        if (request.getDescription() != null) {
            subject.setDescription(request.getDescription());
        }
        if (request.getCredits() != null) {
            subject.setCredits(request.getCredits());
        }
        if (request.getSemester() != null) {
            subject.setSemester(request.getSemester());
        }
        if (request.getRegulation() != null) {
            subject.setRegulation(request.getRegulation());
        }
        if (department != null) {
            subject.setDepartment(department);
        }
        if (faculty != null) {
            subject.setFaculty(faculty);
        }
    }
}
