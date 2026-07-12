package com.secureiq.SecureIQ.department.mapper;

import com.secureiq.SecureIQ.department.dto.DepartmentCreateRequest;
import com.secureiq.SecureIQ.department.dto.DepartmentPatchRequest;
import com.secureiq.SecureIQ.department.dto.DepartmentResponse;
import com.secureiq.SecureIQ.department.dto.DepartmentUpdateRequest;
import com.secureiq.SecureIQ.department.model.Department;
import com.secureiq.SecureIQ.user.mapper.UserMapper;
import com.secureiq.SecureIQ.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    private final UserMapper userMapper;

    public DepartmentMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public DepartmentResponse toResponse(Department department, long totalStudents, long totalFaculty) {
        if (department == null) {
            return null;
        }

        return DepartmentResponse.builder()
                .id(department.getId())
                .departmentName(department.getDepartmentName())
                .departmentCode(department.getDepartmentCode())
                .description(department.getDescription())
                .hod(userMapper.toResponse(department.getHod()))
                .totalStudents(totalStudents)
                .totalFaculty(totalFaculty)
                .createdAt(department.getCreatedAt() != null ? department.getCreatedAt().toString() : null)
                .updatedAt(department.getUpdatedAt() != null ? department.getUpdatedAt().toString() : null)
                .build();
    }

    public Department toEntity(DepartmentCreateRequest request, User hod) {
        if (request == null) {
            return null;
        }

        return Department.builder()
                .departmentName(request.getDepartmentName())
                .departmentCode(request.getDepartmentCode())
                .description(request.getDescription())
                .hod(hod)
                .deleted(false)
                .build();
    }

    public void updateEntity(DepartmentUpdateRequest request, User hod, Department department) {
        if (request == null || department == null) {
            return;
        }
        department.setDepartmentName(request.getDepartmentName());
        department.setDepartmentCode(request.getDepartmentCode());
        department.setDescription(request.getDescription());
        department.setHod(hod);
    }

    public void patchEntity(DepartmentPatchRequest request, User hod, Department department) {
        if (request == null || department == null) {
            return;
        }
        if (request.getDepartmentName() != null) {
            department.setDepartmentName(request.getDepartmentName());
        }
        if (request.getDepartmentCode() != null) {
            department.setDepartmentCode(request.getDepartmentCode());
        }
        if (request.getDescription() != null) {
            department.setDescription(request.getDescription());
        }
        if (hod != null) {
            department.setHod(hod);
        }
    }
}
