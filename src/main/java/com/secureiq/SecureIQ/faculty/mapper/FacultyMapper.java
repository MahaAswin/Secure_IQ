package com.secureiq.SecureIQ.faculty.mapper;

import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.faculty.dto.FacultyCreateRequest;
import com.secureiq.SecureIQ.faculty.dto.FacultyPatchRequest;
import com.secureiq.SecureIQ.faculty.dto.FacultyResponse;
import com.secureiq.SecureIQ.faculty.dto.FacultyUpdateRequest;
import com.secureiq.SecureIQ.faculty.model.Faculty;
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
public class FacultyMapper {

    private final UserMapper userMapper;

    public FacultyMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public FacultyResponse toResponse(Faculty faculty) {
        if (faculty == null) {
            return null;
        }

        FacultyResponse.DepartmentDto deptDto = null;
        if (faculty.getDepartment() != null) {
            deptDto = FacultyResponse.DepartmentDto.builder()
                    .id(faculty.getDepartment().getId())
                    .name(faculty.getDepartment().getDepartmentName())
                    .code(faculty.getDepartment().getDepartmentCode())
                    .build();
        }

        List<FacultyResponse.SubjectDto> subjectDtos = new ArrayList<>();
        if (faculty.getSubjects() != null) {
            subjectDtos = faculty.getSubjects().stream()
                    .map(sub -> FacultyResponse.SubjectDto.builder()
                            .id(sub.getId())
                            .subjectCode(sub.getSubjectCode())
                            .subjectName(sub.getSubjectName())
                            .credits(sub.getCredits())
                            .semester(sub.getSemester())
                            .build())
                    .collect(Collectors.toList());
        }

        return FacultyResponse.builder()
                .id(faculty.getId())
                .user(userMapper.toResponse(faculty.getUser()))
                .employeeId(faculty.getEmployeeId())
                .designation(faculty.getDesignation())
                .qualification(faculty.getQualification())
                .specialization(faculty.getSpecialization())
                .yearsOfExperience(faculty.getYearsOfExperience())
                .officeLocation(faculty.getOfficeLocation())
                .department(deptDto)
                .joiningDate(faculty.getJoiningDate())
                .profileCompleted(faculty.isProfileCompleted())
                .subjects(subjectDtos)
                .createdAt(faculty.getCreatedAt() != null ? faculty.getCreatedAt().toString() : null)
                .updatedAt(faculty.getUpdatedAt() != null ? faculty.getUpdatedAt().toString() : null)
                .build();
    }

    public Faculty toEntity(FacultyCreateRequest request, User user, Department department, Set<Subject> subjects) {
        if (request == null) {
            return null;
        }

        return Faculty.builder()
                .user(user)
                .employeeId(request.getEmployeeId())
                .designation(request.getDesignation())
                .qualification(request.getQualification())
                .specialization(request.getSpecialization())
                .yearsOfExperience(request.getYearsOfExperience())
                .officeLocation(request.getOfficeLocation())
                .department(department)
                .joiningDate(request.getJoiningDate())
                .profileCompleted(false)
                .subjects(subjects != null ? subjects : new HashSet<>())
                .deleted(false)
                .build();
    }

    public void updateEntity(FacultyUpdateRequest request, Department department, Set<Subject> subjects, Faculty faculty) {
        if (request == null || faculty == null) {
            return;
        }
        faculty.setEmployeeId(request.getEmployeeId());
        faculty.setDesignation(request.getDesignation());
        faculty.setQualification(request.getQualification());
        faculty.setSpecialization(request.getSpecialization());
        faculty.setYearsOfExperience(request.getYearsOfExperience());
        faculty.setOfficeLocation(request.getOfficeLocation());
        faculty.setDepartment(department);
        faculty.setJoiningDate(request.getJoiningDate());
        if (request.getProfileCompleted() != null) {
            faculty.setProfileCompleted(request.getProfileCompleted());
        }
        if (subjects != null) {
            faculty.setSubjects(subjects);
        }
    }

    public void patchEntity(FacultyPatchRequest request, Department department, Set<Subject> subjects, Faculty faculty) {
        if (request == null || faculty == null) {
            return;
        }
        if (request.getEmployeeId() != null) {
            faculty.setEmployeeId(request.getEmployeeId());
        }
        if (request.getDesignation() != null) {
            faculty.setDesignation(request.getDesignation());
        }
        if (request.getQualification() != null) {
            faculty.setQualification(request.getQualification());
        }
        if (request.getSpecialization() != null) {
            faculty.setSpecialization(request.getSpecialization());
        }
        if (request.getYearsOfExperience() != null) {
            faculty.setYearsOfExperience(request.getYearsOfExperience());
        }
        if (request.getOfficeLocation() != null) {
            faculty.setOfficeLocation(request.getOfficeLocation());
        }
        if (department != null) {
            faculty.setDepartment(department);
        }
        if (request.getJoiningDate() != null) {
            faculty.setJoiningDate(request.getJoiningDate());
        }
        if (request.getProfileCompleted() != null) {
            faculty.setProfileCompleted(request.getProfileCompleted());
        }
        if (subjects != null) {
            faculty.setSubjects(subjects);
        }
    }
}
