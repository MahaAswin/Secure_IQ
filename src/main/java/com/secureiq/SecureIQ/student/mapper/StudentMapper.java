package com.secureiq.SecureIQ.student.mapper;

import com.secureiq.SecureIQ.student.dto.StudentCreateRequest;
import com.secureiq.SecureIQ.student.dto.StudentPatchRequest;
import com.secureiq.SecureIQ.student.dto.StudentResponse;
import com.secureiq.SecureIQ.student.dto.StudentUpdateRequest;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.student.model.Student;
import com.secureiq.SecureIQ.user.mapper.UserMapper;
import com.secureiq.SecureIQ.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    private final UserMapper userMapper;

    public StudentMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public StudentResponse toResponse(Student student) {
        if (student == null) {
            return null;
        }

        StudentResponse.DepartmentDto deptDto = null;
        if (student.getDepartment() != null) {
            deptDto = StudentResponse.DepartmentDto.builder()
                    .id(student.getDepartment().getId())
                    .name(student.getDepartment().getDepartmentName())
                    .code(student.getDepartment().getDepartmentCode())
                    .build();
        }

        return StudentResponse.builder()
                .id(student.getId())
                .user(userMapper.toResponse(student.getUser()))
                .registerNumber(student.getRegisterNumber())
                .rollNumber(student.getRollNumber())
                .department(deptDto)
                .academicYear(student.getAcademicYear())
                .semester(student.getSemester())
                .section(student.getSection())
                .batch(student.getBatch())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .address(student.getAddress())
                .parentName(student.getParentName())
                .parentPhone(student.getParentPhone())
                .emergencyContact(student.getEmergencyContact())
                .profileCompleted(student.isProfileCompleted())
                .createdAt(student.getCreatedAt() != null ? student.getCreatedAt().toString() : null)
                .updatedAt(student.getUpdatedAt() != null ? student.getUpdatedAt().toString() : null)
                .build();
    }

    public Student toEntity(StudentCreateRequest request, User user, Department department) {
        if (request == null) {
            return null;
        }

        return Student.builder()
                .user(user)
                .registerNumber(request.getRegisterNumber())
                .rollNumber(request.getRollNumber())
                .department(department)
                .academicYear(request.getAcademicYear())
                .semester(request.getSemester())
                .section(request.getSection())
                .batch(request.getBatch())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .parentName(request.getParentName())
                .parentPhone(request.getParentPhone())
                .emergencyContact(request.getEmergencyContact())
                .profileCompleted(request.getProfileCompleted() != null ? request.getProfileCompleted() : false)
                .deleted(false)
                .build();
    }

    public void updateEntity(StudentUpdateRequest request, Department department, Student student) {
        if (request == null || student == null) {
            return;
        }
        student.setRegisterNumber(request.getRegisterNumber());
        student.setRollNumber(request.getRollNumber());
        student.setDepartment(department);
        student.setAcademicYear(request.getAcademicYear());
        student.setSemester(request.getSemester());
        student.setSection(request.getSection());
        student.setBatch(request.getBatch());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setGender(request.getGender());
        student.setAddress(request.getAddress());
        student.setParentName(request.getParentName());
        student.setParentPhone(request.getParentPhone());
        student.setEmergencyContact(request.getEmergencyContact());
        if (request.getProfileCompleted() != null) {
            student.setProfileCompleted(request.getProfileCompleted());
        }
    }

    public void patchEntity(StudentPatchRequest request, Department department, Student student) {
        if (request == null || student == null) {
            return;
        }
        if (request.getRegisterNumber() != null) {
            student.setRegisterNumber(request.getRegisterNumber());
        }
        if (request.getRollNumber() != null) {
            student.setRollNumber(request.getRollNumber());
        }
        if (department != null) {
            student.setDepartment(department);
        }
        if (request.getAcademicYear() != null) {
            student.setAcademicYear(request.getAcademicYear());
        }
        if (request.getSemester() != null) {
            student.setSemester(request.getSemester());
        }
        if (request.getSection() != null) {
            student.setSection(request.getSection());
        }
        if (request.getBatch() != null) {
            student.setBatch(request.getBatch());
        }
        if (request.getDateOfBirth() != null) {
            student.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            student.setGender(request.getGender());
        }
        if (request.getAddress() != null) {
            student.setAddress(request.getAddress());
        }
        if (request.getParentName() != null) {
            student.setParentName(request.getParentName());
        }
        if (request.getParentPhone() != null) {
            student.setParentPhone(request.getParentPhone());
        }
        if (request.getEmergencyContact() != null) {
            student.setEmergencyContact(request.getEmergencyContact());
        }
        if (request.getProfileCompleted() != null) {
            student.setProfileCompleted(request.getProfileCompleted());
        }
    }
}
